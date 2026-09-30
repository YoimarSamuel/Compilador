package modelo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Analizador Léxico para el compilador
 * Convierte código fuente en una secuencia de tokens
 * @author Ardila App
 */
public class AnalizadorLexico {
    private String codigoFuente;
    private int posicion;
    private int linea;
    private int columna;
    private char caracterActual;
    private List<Token> tokens;
    private List<String> errores;
    
    // Palabras reservadas de Java
    private static final Set<String> PALABRAS_RESERVADAS = new HashSet<>(Arrays.asList(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
        "class", "const", "continue", "default", "do", "double", "else", "enum",
        "extends", "final", "finally", "float", "for", "goto", "if", "implements",
        "import", "instanceof", "int", "interface", "long", "native", "new", "package",
        "private", "protected", "public", "return", "short", "static", "strictfp",
        "super", "switch", "synchronized", "this", "throw", "throws", "transient",
        "try", "void", "volatile", "while", "true", "false", "null"
    ));
    
    /**
     * Constructor del Analizador Léxico
     * @param codigoFuente Código fuente a analizar
     */
    public AnalizadorLexico(String codigoFuente) {
        this.codigoFuente = codigoFuente;
        this.posicion = 0;
        this.linea = 1;
        this.columna = 1;
        this.tokens = new ArrayList<>();
        this.errores = new ArrayList<>();
        this.caracterActual = codigoFuente.length() > 0 ? codigoFuente.charAt(0) : '\0';
    }
    
    /**
     * Analiza el código fuente y genera la lista de tokens
     * @return Lista de tokens identificados
     */
    public List<Token> analizar() {
        tokens.clear();
        errores.clear();
        
        while (caracterActual != '\0') {
            // Saltar espacios en blanco
            if (Character.isWhitespace(caracterActual)) {
                if (caracterActual == '\n') {
                    linea++;
                    columna = 1;
                } else {
                    columna++;
                }
                avanzar();
                continue;
            }
            
            // Comentarios
            if (caracterActual == '/' && mirarSiguiente() == '/') {
                procesarComentarioLinea();
                continue;
            }
            
            if (caracterActual == '/' && mirarSiguiente() == '*') {
                procesarComentarioBloque();
                continue;
            }
            
            // Identificadores y palabras reservadas
            if (Character.isLetter(caracterActual) || caracterActual == '_') {
                procesarIdentificadorOPalabraReservada();
                continue;
            }
            
            // Números
            if (Character.isDigit(caracterActual)) {
                procesarNumero();
                continue;
            }
            
            // Cadenas
            if (caracterActual == '"') {
                procesarCadena();
                continue;
            }
            
            // Caracteres
            if (caracterActual == '\'') {
                procesarCaracter();
                continue;
            }
            
            // Operadores y delimitadores
            if (procesarOperadorODelimitador()) {
                continue;
            }
            
            // Si no se reconoce, es un error
            int colInicio = columna;
            String lexemaError = String.valueOf(caracterActual);
            avanzar();
            tokens.add(new Token(TipoToken.ERROR, lexemaError, linea, colInicio));
            errores.add(String.format("Caracter no reconocido '%s' en línea %d, columna %d", 
                                     lexemaError, linea, colInicio));
        }
        
        // Agregar token de fin de archivo
        tokens.add(new Token(TipoToken.FIN_DE_ARCHIVO, "", linea, columna));
        
        return tokens;
    }
    
    private void avanzar() {
        posicion++;
        if (posicion < codigoFuente.length()) {
            caracterActual = codigoFuente.charAt(posicion);
        } else {
            caracterActual = '\0';
        }
    }
    
    private char mirarSiguiente() {
        if (posicion + 1 < codigoFuente.length()) {
            return codigoFuente.charAt(posicion + 1);
        }
        return '\0';
    }
    
    private void procesarComentarioLinea() {
        int colInicio = columna;
        StringBuilder comentario = new StringBuilder();
        
        while (caracterActual != '\n' && caracterActual != '\0') {
            comentario.append(caracterActual);
            columna++;
            avanzar();
        }
        
        tokens.add(new Token(TipoToken.COMENTARIO_LINEA, comentario.toString(), linea, colInicio));
    }
    
    private void procesarComentarioBloque() {
        int colInicio = columna;
        int lineaInicio = linea;
        StringBuilder comentario = new StringBuilder();
        
        // Consumir /*
        comentario.append(caracterActual);
        columna++;
        avanzar();
        comentario.append(caracterActual);
        columna++;
        avanzar();
        
        while (caracterActual != '\0') {
            if (caracterActual == '*' && mirarSiguiente() == '/') {
                comentario.append(caracterActual);
                columna++;
                avanzar();
                comentario.append(caracterActual);
                columna++;
                avanzar();
                tokens.add(new Token(TipoToken.COMENTARIO_BLOQUE, comentario.toString(), lineaInicio, colInicio));
                return;
            }
            
            if (caracterActual == '\n') {
                linea++;
                columna = 1;
            } else {
                columna++;
            }
            
            comentario.append(caracterActual);
            avanzar();
        }
        
        // Comentario no cerrado
        tokens.add(new Token(TipoToken.ERROR, comentario.toString(), lineaInicio, colInicio));
        errores.add(String.format("Comentario de bloque no cerrado iniciado en línea %d, columna %d", 
                                 lineaInicio, colInicio));
    }
    
    private void procesarIdentificadorOPalabraReservada() {
        int colInicio = columna;
        StringBuilder lexema = new StringBuilder();
        
        while (Character.isLetterOrDigit(caracterActual) || caracterActual == '_') {
            lexema.append(caracterActual);
            columna++;
            avanzar();
        }
        
        String texto = lexema.toString();
        TipoToken tipo;
        
        if (PALABRAS_RESERVADAS.contains(texto)) {
            tipo = TipoToken.PALABRA_RESERVADA;
        } else if (texto.equals("true") || texto.equals("false")) {
            tipo = TipoToken.BOOLEANO;
        } else {
            tipo = TipoToken.IDENTIFICADOR;
        }
        
        tokens.add(new Token(tipo, texto, linea, colInicio));
    }
    
    private void procesarNumero() {
        int colInicio = columna;
        StringBuilder numero = new StringBuilder();
        boolean esDecimal = false;
        
        while (Character.isDigit(caracterActual)) {
            numero.append(caracterActual);
            columna++;
            avanzar();
        }
        
        // Verificar si es decimal
        if (caracterActual == '.' && Character.isDigit(mirarSiguiente())) {
            esDecimal = true;
            numero.append(caracterActual);
            columna++;
            avanzar();
            
            while (Character.isDigit(caracterActual)) {
                numero.append(caracterActual);
                columna++;
                avanzar();
            }
        }
        
        // Notación científica (e, E)
        if (caracterActual == 'e' || caracterActual == 'E') {
            esDecimal = true;
            numero.append(caracterActual);
            columna++;
            avanzar();
            
            if (caracterActual == '+' || caracterActual == '-') {
                numero.append(caracterActual);
                columna++;
                avanzar();
            }
            
            while (Character.isDigit(caracterActual)) {
                numero.append(caracterActual);
                columna++;
                avanzar();
            }
        }
        
        TipoToken tipo = esDecimal ? TipoToken.NUMERO_DECIMAL : TipoToken.NUMERO_ENTERO;
        tokens.add(new Token(tipo, numero.toString(), linea, colInicio));
    }
    
    private void procesarCadena() {
        int colInicio = columna;
        int lineaInicio = linea;
        StringBuilder cadena = new StringBuilder();
        
        // Consumir comilla inicial
        cadena.append(caracterActual);
        columna++;
        avanzar();
        
        while (caracterActual != '"' && caracterActual != '\0' && caracterActual != '\n') {
            if (caracterActual == '\\') {
                cadena.append(caracterActual);
                columna++;
                avanzar();
                
                if (caracterActual != '\0') {
                    cadena.append(caracterActual);
                    columna++;
                    avanzar();
                }
            } else {
                cadena.append(caracterActual);
                columna++;
                avanzar();
            }
        }
        
        if (caracterActual == '"') {
            cadena.append(caracterActual);
            columna++;
            avanzar();
            tokens.add(new Token(TipoToken.CADENA, cadena.toString(), lineaInicio, colInicio));
        } else {
            tokens.add(new Token(TipoToken.ERROR, cadena.toString(), lineaInicio, colInicio));
            errores.add(String.format("Cadena no cerrada iniciada en línea %d, columna %d", 
                                     lineaInicio, colInicio));
        }
    }
    
    private void procesarCaracter() {
        int colInicio = columna;
        int lineaInicio = linea;
        StringBuilder caracter = new StringBuilder();
        
        // Consumir comilla simple inicial
        caracter.append(caracterActual);
        columna++;
        avanzar();
        
        int caracteres = 0;
        while (caracterActual != '\'' && caracterActual != '\0' && caracterActual != '\n') {
            caracter.append(caracterActual);
            caracteres++;
            
            if (caracterActual == '\\') {
                columna++;
                avanzar();
                if (caracterActual != '\0' && caracterActual != '\n') {
                    caracter.append(caracterActual);
                    columna++;
                    avanzar();
                }
            } else {
                columna++;
                avanzar();
            }
        }
        
        if (caracterActual == '\'') {
            caracter.append(caracterActual);
            columna++;
            avanzar();
            
            if (caracteres == 0 || caracteres > 2) {
                tokens.add(new Token(TipoToken.ERROR, caracter.toString(), lineaInicio, colInicio));
                errores.add(String.format("Literal de caracter inválido en línea %d, columna %d", 
                                         lineaInicio, colInicio));
            } else {
                tokens.add(new Token(TipoToken.CARACTER, caracter.toString(), lineaInicio, colInicio));
            }
        } else {
            tokens.add(new Token(TipoToken.ERROR, caracter.toString(), lineaInicio, colInicio));
            errores.add(String.format("Literal de caracter no cerrado iniciado en línea %d, columna %d", 
                                     lineaInicio, colInicio));
        }
    }
    
    private boolean procesarOperadorODelimitador() {
        int colInicio = columna;
        char c = caracterActual;
        char siguiente = mirarSiguiente();
        
        // Operadores de dos caracteres
        if (c == '+' && siguiente == '+') {
            columna += 2;
            avanzar();
            avanzar();
            tokens.add(new Token(TipoToken.INCREMENTO, "++", linea, colInicio));
            return true;
        }
        if (c == '-' && siguiente == '-') {
            columna += 2;
            avanzar();
            avanzar();
            tokens.add(new Token(TipoToken.DECREMENTO, "--", linea, colInicio));
            return true;
        }
        if (c == '=' && siguiente == '=') {
            columna += 2;
            avanzar();
            avanzar();
            tokens.add(new Token(TipoToken.OPERADOR_IGUAL, "==", linea, colInicio));
            return true;
        }
        if (c == '!' && siguiente == '=') {
            columna += 2;
            avanzar();
            avanzar();
            tokens.add(new Token(TipoToken.OPERADOR_DIFERENTE, "!=", linea, colInicio));
            return true;
        }
        if (c == '>' && siguiente == '=') {
            columna += 2;
            avanzar();
            avanzar();
            tokens.add(new Token(TipoToken.OPERADOR_MAYOR_IGUAL, ">=", linea, colInicio));
            return true;
        }
        if (c == '<' && siguiente == '=') {
            columna += 2;
            avanzar();
            avanzar();
            tokens.add(new Token(TipoToken.OPERADOR_MENOR_IGUAL, "<=", linea, colInicio));
            return true;
        }
        if (c == '&' && siguiente == '&') {
            columna += 2;
            avanzar();
            avanzar();
            tokens.add(new Token(TipoToken.OPERADOR_AND, "&&", linea, colInicio));
            return true;
        }
        if (c == '|' && siguiente == '|') {
            columna += 2;
            avanzar();
            avanzar();
            tokens.add(new Token(TipoToken.OPERADOR_OR, "||", linea, colInicio));
            return true;
        }
        if (c == '+' && siguiente == '=') {
            columna += 2;
            avanzar();
            avanzar();
            tokens.add(new Token(TipoToken.ASIGNACION_SUMA, "+=", linea, colInicio));
            return true;
        }
        if (c == '-' && siguiente == '=') {
            columna += 2;
            avanzar();
            avanzar();
            tokens.add(new Token(TipoToken.ASIGNACION_RESTA, "-=", linea, colInicio));
            return true;
        }
        if (c == '*' && siguiente == '=') {
            columna += 2;
            avanzar();
            avanzar();
            tokens.add(new Token(TipoToken.ASIGNACION_MULTIPLICACION, "*=", linea, colInicio));
            return true;
        }
        if (c == '/' && siguiente == '=') {
            columna += 2;
            avanzar();
            avanzar();
            tokens.add(new Token(TipoToken.ASIGNACION_DIVISION, "/=", linea, colInicio));
            return true;
        }
        
        // Operadores de un caracter
        switch (c) {
            case '+':
                tokens.add(new Token(TipoToken.OPERADOR_SUMA, "+", linea, colInicio));
                columna++;
                avanzar();
                return true;
            case '-':
                tokens.add(new Token(TipoToken.OPERADOR_RESTA, "-", linea, colInicio));
                columna++;
                avanzar();
                return true;
            case '*':
                tokens.add(new Token(TipoToken.OPERADOR_MULTIPLICACION, "*", linea, colInicio));
                columna++;
                avanzar();
                return true;
            case '/':
                tokens.add(new Token(TipoToken.OPERADOR_DIVISION, "/", linea, colInicio));
                columna++;
                avanzar();
                return true;
            case '%':
                tokens.add(new Token(TipoToken.OPERADOR_MODULO, "%", linea, colInicio));
                columna++;
                avanzar();
                return true;
            case '=':
                tokens.add(new Token(TipoToken.ASIGNACION, "=", linea, colInicio));
                columna++;
                avanzar();
                return true;
            case '>':
                tokens.add(new Token(TipoToken.OPERADOR_MAYOR, ">", linea, colInicio));
                columna++;
                avanzar();
                return true;
            case '<':
                tokens.add(new Token(TipoToken.OPERADOR_MENOR, "<", linea, colInicio));
                columna++;
                avanzar();
                return true;
            case '!':
                tokens.add(new Token(TipoToken.OPERADOR_NOT, "!", linea, colInicio));
                columna++;
                avanzar();
                return true;
            case '(':
                tokens.add(new Token(TipoToken.PARENTESIS_IZQ, "(", linea, colInicio));
                columna++;
                avanzar();
                return true;
            case ')':
                tokens.add(new Token(TipoToken.PARENTESIS_DER, ")", linea, colInicio));
                columna++;
                avanzar();
                return true;
            case '{':
                tokens.add(new Token(TipoToken.LLAVE_IZQ, "{", linea, colInicio));
                columna++;
                avanzar();
                return true;
            case '}':
                tokens.add(new Token(TipoToken.LLAVE_DER, "}", linea, colInicio));
                columna++;
                avanzar();
                return true;
            case '[':
                tokens.add(new Token(TipoToken.CORCHETE_IZQ, "[", linea, colInicio));
                columna++;
                avanzar();
                return true;
            case ']':
                tokens.add(new Token(TipoToken.CORCHETE_DER, "]", linea, colInicio));
                columna++;
                avanzar();
                return true;
            case ';':
                tokens.add(new Token(TipoToken.PUNTO_COMA, ";", linea, colInicio));
                columna++;
                avanzar();
                return true;
            case ',':
                tokens.add(new Token(TipoToken.COMA, ",", linea, colInicio));
                columna++;
                avanzar();
                return true;
            case '.':
                tokens.add(new Token(TipoToken.PUNTO, ".", linea, colInicio));
                columna++;
                avanzar();
                return true;
            case ':':
                tokens.add(new Token(TipoToken.DOS_PUNTOS, ":", linea, colInicio));
                columna++;
                avanzar();
                return true;
        }
        
        return false;
    }
    
    public List<Token> getTokens() {
        return tokens;
    }
    
    public List<String> getErrores() {
        return errores;
    }
    
    public boolean tieneErrores() {
        return !errores.isEmpty();
    }
}
