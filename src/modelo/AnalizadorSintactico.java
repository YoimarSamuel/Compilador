package modelo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Analizador Sintáctico (parser descendente recursivo) para el compilador.
 * Recibe la lista de tokens generada por AnalizadorLexico y valida que
 * cumplan con la gramática definida abajo.
 *
 * GRAMÁTICA (BNF) — subconjunto de tipo Java:
 *
 *   programa            -> sentencia* FIN_DE_ARCHIVO
 *
 *   sentencia           -> bloque
 *                        | ';'                                (sentencia vacía)
 *                        | declaracion
 *                        | 'if' '(' expresion ')' sentencia ('else' sentencia)?
 *                        | 'while' '(' expresion ')' sentencia
 *                        | 'for' '(' forInit? ';' expresion? ';' forUpdate? ')' sentencia
 *                        | 'return' expresion? ';'
 *                        | sentenciaDesdeIdentificador ';'      (asignación, llamada, ++/--)
 *
 *   bloque              -> '{' sentencia* '}'
 *
 *   declaracion         -> tipoDato IDENTIFICADOR ('=' expresion)?
 *                           (',' IDENTIFICADOR ('=' expresion)?)* ';'
 *   tipoDato            -> 'int' | 'double' | 'float' | 'boolean'
 *                        | 'char' | 'long' | 'short' | 'byte'
 *
 *   sentenciaDesdeIdentificador
 *                       -> IDENTIFICADOR ('.' IDENTIFICADOR)*
 *                          ( asignacionCompuesta expresion
 *                          | '(' argumentos? ')'
 *                          | '++' | '--' )
 *
 *   expresion           -> expresionOr
 *   expresionOr          -> expresionAnd ('||' expresionAnd)*
 *   expresionAnd         -> expresionIgualdad ('&&' expresionIgualdad)*
 *   expresionIgualdad    -> expresionRelacional (('=='|'!=') expresionRelacional)*
 *   expresionRelacional  -> expresionAditiva (('<'|'>'|'<='|'>=') expresionAditiva)*
 *   expresionAditiva     -> expresionMultiplicativa (('+'|'-') expresionMultiplicativa)*
 *   expresionMultiplicativa -> expresionUnaria (('*'|'/'|'%') expresionUnaria)*
 *   expresionUnaria      -> ('!' | '-') expresionUnaria | primario
 *   primario             -> NUMERO_ENTERO | NUMERO_DECIMAL | CADENA | CARACTER | BOOLEANO
 *                        | IDENTIFICADOR ('.' IDENTIFICADOR)* ( '(' argumentos? ')' | ('++'|'--')? )
 *                        | '(' expresion ')'
 *
 *   argumentos           -> expresion (',' expresion)*
 *
 *
 * @author (equipo - análisis sintáctico)
 */
public class AnalizadorSintactico {

    private final List<Token> tokens;
    private int posicion;
    private final List<String> errores;

    private static final Set<String> TIPOS_DATO = new HashSet<>(Arrays.asList(
        "int", "double", "float", "boolean", "char", "long", "short", "byte"
    ));

    /**
     * @param tokensLexicos tokens ya generados por AnalizadorLexico.analizar()
     */
    public AnalizadorSintactico(List<Token> tokensLexicos) {
        // Filtramos tokens que no participan en la gramática sintáctica
        this.tokens = new ArrayList<>();
        for (Token t : tokensLexicos) {
            if (t.getTipo() != TipoToken.COMENTARIO_LINEA
                    && t.getTipo() != TipoToken.COMENTARIO_BLOQUE
                    && t.getTipo() != TipoToken.ESPACIO_BLANCO) {
                this.tokens.add(t);
            }
        }
        this.posicion = 0;
        this.errores = new ArrayList<>();
    }

    /**
     * Ejecuta el análisis sintáctico completo.
     * @return true si no se encontraron errores sintácticos
     */
    public boolean analizar() {
        errores.clear();
        posicion = 0;

        while (!esFinDeArchivo()) {
            try {
                sentencia();
            } catch (ErrorSintactico e) {
                errores.add(e.getMessage());
                sincronizar();
            }
        }
        return errores.isEmpty();
    }

    public List<String> getErrores() {
        return errores;
    }

    public boolean tieneErrores() {
        return !errores.isEmpty();
    }

    // ---------------------------------------------------------------
    // Recuperación de errores (modo pánico): avanza hasta ';' o '}' 
    // para poder seguir buscando más errores en vez de detenerse en el primero
    // ---------------------------------------------------------------
    private void sincronizar() {
        while (!esFinDeArchivo()) {
            if (chequear(TipoToken.PUNTO_COMA)) {
                avanzar();
                return;
            }
            if (chequear(TipoToken.LLAVE_DER)) {
                return;
            }
            avanzar();
        }
    }

    // ---------------------------------------------------------------
    // Utilidades básicas del parser
    // ---------------------------------------------------------------
    private Token tokenActual() {
        return tokens.get(posicion);
    }

    private boolean esFinDeArchivo() {
        return tokenActual().getTipo() == TipoToken.FIN_DE_ARCHIVO;
    }

    private Token avanzar() {
        Token t = tokenActual();
        if (!esFinDeArchivo()) {
            posicion++;
        }
        return t;
    }

    private boolean chequear(TipoToken tipo) {
        return tokenActual().getTipo() == tipo;
    }

    private boolean esPalabraReservada(String palabra) {
        Token t = tokenActual();
        return t.getTipo() == TipoToken.PALABRA_RESERVADA && t.getLexema().equals(palabra);
    }

    private boolean esTipoDato() {
        Token t = tokenActual();
        return t.getTipo() == TipoToken.PALABRA_RESERVADA && TIPOS_DATO.contains(t.getLexema());
    }

    private boolean esAsignacionCompuesta() {
        TipoToken t = tokenActual().getTipo();
        return t == TipoToken.ASIGNACION || t == TipoToken.ASIGNACION_SUMA
                || t == TipoToken.ASIGNACION_RESTA || t == TipoToken.ASIGNACION_MULTIPLICACION
                || t == TipoToken.ASIGNACION_DIVISION;
    }

    private Token match(TipoToken tipoEsperado, String descripcion) {
        if (chequear(tipoEsperado)) {
            return avanzar();
        }
        error("Se esperaba " + descripcion + " pero se encontró '" + tokenActual().getLexema() + "'");
        return null; // no se alcanza: error() lanza excepción
    }

    private void error(String mensaje) {
        Token t = tokenActual();
        throw new ErrorSintactico(String.format(
                "Línea %d, columna %d: %s", t.getLinea(), t.getColumna(), mensaje));
    }

    // ---------------------------------------------------------------
    // sentencia -> bloque | ';' | declaracion | if | while | for | return
    //            | sentenciaDesdeIdentificador ';'
    // ---------------------------------------------------------------
    private void sentencia() {
        if (chequear(TipoToken.LLAVE_IZQ)) {
            bloque();
        } else if (chequear(TipoToken.PUNTO_COMA)) {
            avanzar(); // sentencia vacía
        } else if (esTipoDato()) {
            declaracion();
        } else if (esPalabraReservada("if")) {
            sentenciaSi();
        } else if (esPalabraReservada("while")) {
            sentenciaMientras();
        } else if (esPalabraReservada("for")) {
            sentenciaPara();
        } else if (esPalabraReservada("return")) {
            sentenciaReturn();
        } else if (chequear(TipoToken.IDENTIFICADOR)) {
            sentenciaDesdeIdentificador();
            match(TipoToken.PUNTO_COMA, "';'");
        } else {
            error("Se esperaba una sentencia válida, se encontró '" + tokenActual().getLexema() + "'");
        }
    }

    private void bloque() {
        match(TipoToken.LLAVE_IZQ, "'{'");
        while (!chequear(TipoToken.LLAVE_DER) && !esFinDeArchivo()) {
            try {
                sentencia();
            } catch (ErrorSintactico e) {
                errores.add(e.getMessage());
                sincronizar();
            }
        }
        match(TipoToken.LLAVE_DER, "'}'");
    }

    // declaracion -> tipoDato IDENTIFICADOR ('=' expresion)? (',' IDENTIFICADOR ('=' expresion)?)* ';'
    private void declaracion() {
        avanzar(); // consume el tipo de dato (int, double, boolean...)
        match(TipoToken.IDENTIFICADOR, "un identificador");
        if (chequear(TipoToken.ASIGNACION)) {
            avanzar();
            expresion();
        }
        while (chequear(TipoToken.COMA)) {
            avanzar();
            match(TipoToken.IDENTIFICADOR, "un identificador");
            if (chequear(TipoToken.ASIGNACION)) {
                avanzar();
                expresion();
            }
        }
        match(TipoToken.PUNTO_COMA, "';'");
    }

    // if '(' expresion ')' sentencia ('else' sentencia)?
    private void sentenciaSi() {
        avanzar(); // 'if'
        match(TipoToken.PARENTESIS_IZQ, "'('");
        expresion();
        match(TipoToken.PARENTESIS_DER, "')'");
        sentencia();
        if (esPalabraReservada("else")) {
            avanzar();
            sentencia();
        }
    }

    // while '(' expresion ')' sentencia
    private void sentenciaMientras() {
        avanzar(); // 'while'
        match(TipoToken.PARENTESIS_IZQ, "'('");
        expresion();
        match(TipoToken.PARENTESIS_DER, "')'");
        sentencia();
    }

    // for '(' forInit? ';' expresion? ';' forUpdate? ')' sentencia
    private void sentenciaPara() {
        avanzar(); // 'for'
        match(TipoToken.PARENTESIS_IZQ, "'('");

        // inicialización
        if (esTipoDato()) {
            avanzar();
            match(TipoToken.IDENTIFICADOR, "un identificador");
            if (chequear(TipoToken.ASIGNACION)) {
                avanzar();
                expresion();
            }
        } else if (!chequear(TipoToken.PUNTO_COMA)) {
            sentenciaDesdeIdentificador();
        }
        match(TipoToken.PUNTO_COMA, "';'");

        // condición
        if (!chequear(TipoToken.PUNTO_COMA)) {
            expresion();
        }
        match(TipoToken.PUNTO_COMA, "';'");

        // actualización
        if (!chequear(TipoToken.PARENTESIS_DER)) {
            sentenciaDesdeIdentificador();
        }
        match(TipoToken.PARENTESIS_DER, "')'");

        sentencia();
    }

    // return expresion? ';'
    private void sentenciaReturn() {
        avanzar(); // 'return'
        if (!chequear(TipoToken.PUNTO_COMA)) {
            expresion();
        }
        match(TipoToken.PUNTO_COMA, "';'");
    }

    // Maneja: asignación (=, +=, -=, *=, /=), llamada a método (p.ej. System.out.println(x))
    // e incremento/decremento (x++, x--). NO consume el ';' final (lo hace el llamador).
    private void sentenciaDesdeIdentificador() {
        match(TipoToken.IDENTIFICADOR, "un identificador");
        while (chequear(TipoToken.PUNTO)) {
            avanzar();
            match(TipoToken.IDENTIFICADOR, "un identificador después de '.'");
        }

        if (esAsignacionCompuesta()) {
            avanzar();
            expresion();
        } else if (chequear(TipoToken.PARENTESIS_IZQ)) {
            avanzar();
            if (!chequear(TipoToken.PARENTESIS_DER)) {
                argumentos();
            }
            match(TipoToken.PARENTESIS_DER, "')'");
        } else if (chequear(TipoToken.INCREMENTO) || chequear(TipoToken.DECREMENTO)) {
            avanzar();
        } else {
            error("Se esperaba '=', un operador de asignación, '(', '++' o '--' después del identificador");
        }
    }

    // argumentos -> expresion (',' expresion)*
    private void argumentos() {
        expresion();
        while (chequear(TipoToken.COMA)) {
            avanzar();
            expresion();
        }
    }

    // ---------------------------------------------------------------
    // Expresiones, de menor a mayor precedencia
    // ---------------------------------------------------------------
    private void expresion() {
        expresionOr();
    }

    private void expresionOr() {
        expresionAnd();
        while (chequear(TipoToken.OPERADOR_OR)) {
            avanzar();
            expresionAnd();
        }
    }

    private void expresionAnd() {
        expresionIgualdad();
        while (chequear(TipoToken.OPERADOR_AND)) {
            avanzar();
            expresionIgualdad();
        }
    }

    private void expresionIgualdad() {
        expresionRelacional();
        while (chequear(TipoToken.OPERADOR_IGUAL) || chequear(TipoToken.OPERADOR_DIFERENTE)) {
            avanzar();
            expresionRelacional();
        }
    }

    private void expresionRelacional() {
        expresionAditiva();
        while (chequear(TipoToken.OPERADOR_MAYOR) || chequear(TipoToken.OPERADOR_MENOR)
                || chequear(TipoToken.OPERADOR_MAYOR_IGUAL) || chequear(TipoToken.OPERADOR_MENOR_IGUAL)) {
            avanzar();
            expresionAditiva();
        }
    }

    private void expresionAditiva() {
        expresionMultiplicativa();
        while (chequear(TipoToken.OPERADOR_SUMA) || chequear(TipoToken.OPERADOR_RESTA)) {
            avanzar();
            expresionMultiplicativa();
        }
    }

    private void expresionMultiplicativa() {
        expresionUnaria();
        while (chequear(TipoToken.OPERADOR_MULTIPLICACION) || chequear(TipoToken.OPERADOR_DIVISION)
                || chequear(TipoToken.OPERADOR_MODULO)) {
            avanzar();
            expresionUnaria();
        }
    }

    private void expresionUnaria() {
        if (chequear(TipoToken.OPERADOR_NOT) || chequear(TipoToken.OPERADOR_RESTA)) {
            avanzar();
            expresionUnaria();
        } else {
            primario();
        }
    }

    // primario -> literal | IDENTIFICADOR(...) | '(' expresion ')'
    private void primario() {
        Token t = tokenActual();

        switch (t.getTipo()) {
            case NUMERO_ENTERO:
            case NUMERO_DECIMAL:
            case CADENA:
            case CARACTER:
            case BOOLEANO:
                avanzar();
                return;

            case IDENTIFICADOR:
                avanzar();
                while (chequear(TipoToken.PUNTO)) {
                    avanzar();
                    match(TipoToken.IDENTIFICADOR, "un identificador después de '.'");
                }
                if (chequear(TipoToken.PARENTESIS_IZQ)) {
                    avanzar();
                    if (!chequear(TipoToken.PARENTESIS_DER)) {
                        argumentos();
                    }
                    match(TipoToken.PARENTESIS_DER, "')'");
                } else if (chequear(TipoToken.INCREMENTO) || chequear(TipoToken.DECREMENTO)) {
                    avanzar();
                }
                return;

            case PARENTESIS_IZQ:
                avanzar();
                expresion();
                match(TipoToken.PARENTESIS_DER, "')'");
                return;

            default:
                error("Se esperaba un valor (número, identificador, cadena, etc.) pero se encontró '"
                        + t.getLexema() + "'");
        }
    }
}

/**
 * Excepción interna usada para control de flujo dentro del parser.
 * Se captura en analizar()/bloque() para poder reportar varios errores
 * en una sola pasada (modo pánico), en vez de detenerse en el primero.
 */
class ErrorSintactico extends RuntimeException {
    public ErrorSintactico(String mensaje) {
        super(mensaje);
    }
}
