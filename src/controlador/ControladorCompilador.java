package controlador;

import modelo.AnalizadorLexico;
import modelo.AnalizadorSintactico;
import modelo.Token;
import modelo.TipoToken;
import java.util.List;

/**
 * Controlador que maneja la lógica del compilador
 * Conecta la vista con el modelo
 * @author Ardila App
 */
public class ControladorCompilador {
    
    /**
     * Analiza el código fuente y retorna el resultado formateado
     * @param codigoFuente Código a analizar
     * @return Resultado del análisis léxico
     */
    public String analizarCodigoFuente(String codigoFuente) {
        if (codigoFuente == null || codigoFuente.trim().isEmpty()) {
            return "Error: No hay código para analizar.";
        }
        
        AnalizadorLexico analizador = new AnalizadorLexico(codigoFuente);
        List<Token> tokens = analizador.analizar();
        
        StringBuilder resultado = new StringBuilder();
        resultado.append("═══════════════════════════════════════════════════════════\n");
        resultado.append("           ANÁLISIS LÉXICO - RESULTADOS\n");
        resultado.append("═══════════════════════════════════════════════════════════\n\n");
        
        // Mostrar estadísticas
        int totalTokens = tokens.size();
        int errores = analizador.getErrores().size();
        resultado.append(String.format("Total de tokens identificados: %d\n", totalTokens));
        resultado.append(String.format("Errores encontrados: %d\n", errores));
        resultado.append("\n");
        
        // Mostrar errores si existen
        if (analizador.tieneErrores()) {
            resultado.append("───────────────────────────────────────────────────────────\n");
            resultado.append("ERRORES LÉXICOS:\n");
            resultado.append("───────────────────────────────────────────────────────────\n");
            for (String error : analizador.getErrores()) {
                resultado.append("❌ ").append(error).append("\n");
            }
            resultado.append("\n");
        }
        
        // Mostrar tokens
        resultado.append("───────────────────────────────────────────────────────────\n");
        resultado.append("TOKENS IDENTIFICADOS:\n");
        resultado.append("───────────────────────────────────────────────────────────\n\n");
        
        int contador = 1;
        for (Token token : tokens) {
            // No mostrar espacios en blanco ni el token de fin de archivo en detalle
            if (token.getTipo() != TipoToken.ESPACIO_BLANCO && 
                token.getTipo() != TipoToken.FIN_DE_ARCHIVO) {
                resultado.append(String.format("%3d. %s\n", contador++, token.toFormattedString()));
            }
        }
        
        resultado.append("\n═══════════════════════════════════════════════════════════\n");
        resultado.append("           ANÁLISIS COMPLETADO\n");
        resultado.append("═══════════════════════════════════════════════════════════\n");
        
        return resultado.toString();
    }
    
    /**
     * Genera la tabla de símbolos a partir del código fuente
     * @param codigoFuente Código a analizar
     * @return Tabla de símbolos formateada
     */
    public String generarTablaSimbolos(String codigoFuente) {
        if (codigoFuente == null || codigoFuente.trim().isEmpty()) {
            return "Error: No hay código para analizar.";
        }
        
        AnalizadorLexico analizador = new AnalizadorLexico(codigoFuente);
        List<Token> tokens = analizador.analizar();
        
        StringBuilder resultado = new StringBuilder();
        resultado.append("═══════════════════════════════════════════════════════════\n");
        resultado.append("              TABLA DE SÍMBOLOS\n");
        resultado.append("═══════════════════════════════════════════════════════════\n\n");
        
        // Encabezados
        resultado.append(String.format("%-5s %-25s %-30s %-10s %-10s\n", 
                                       "No.", "TIPO", "LEXEMA", "LÍNEA", "COLUMNA"));
        resultado.append("───────────────────────────────────────────────────────────\n");
        
        int contador = 1;
        for (Token token : tokens) {
            // Filtrar tokens relevantes para la tabla de símbolos
            if (token.getTipo() != TipoToken.ESPACIO_BLANCO && 
                token.getTipo() != TipoToken.FIN_DE_ARCHIVO &&
                token.getTipo() != TipoToken.COMENTARIO_LINEA &&
                token.getTipo() != TipoToken.COMENTARIO_BLOQUE) {
                
                String lexemaRecortado = token.getLexema();
                if (lexemaRecortado.length() > 28) {
                    lexemaRecortado = lexemaRecortado.substring(0, 25) + "...";
                }
                
                resultado.append(String.format("%-5d %-25s %-30s %-10d %-10d\n", 
                                               contador++,
                                               token.getTipo().toString(),
                                               lexemaRecortado,
                                               token.getLinea(),
                                               token.getColumna()));
            }
        }
        
        resultado.append("\n═══════════════════════════════════════════════════════════\n");
        resultado.append(String.format("Total de símbolos: %d\n", contador - 1));
        resultado.append("═══════════════════════════════════════════════════════════\n");
        
        return resultado.toString();
    }
    
    /**
     * Analiza sintácticamente el código fuente y retorna el resultado formateado.
     * Primero corre el análisis léxico (si tiene errores léxicos, se detiene ahí);
     * si los tokens son válidos léxicamente, corre el parser descendente recursivo.
     * @param codigoFuente Código a analizar
     * @return Resultado del análisis sintáctico
     */
    public String analizarSintaxis(String codigoFuente) {
        if (codigoFuente == null || codigoFuente.trim().isEmpty()) {
            return "Error: No hay código para analizar.";
        }

        AnalizadorLexico lexico = new AnalizadorLexico(codigoFuente);
        List<Token> tokens = lexico.analizar();

        StringBuilder resultado = new StringBuilder();
        resultado.append("═══════════════════════════════════════════════════════════\n");
        resultado.append("           ANÁLISIS SINTÁCTICO - RESULTADOS\n");
        resultado.append("═══════════════════════════════════════════════════════════\n\n");

        if (lexico.tieneErrores()) {
            resultado.append("⚠ Hay errores léxicos; corríjalos antes de analizar la sintaxis:\n\n");
            for (String err : lexico.getErrores()) {
                resultado.append("  ❌ ").append(err).append("\n");
            }
            return resultado.toString();
        }

        AnalizadorSintactico sintactico = new AnalizadorSintactico(tokens);
        boolean valido = sintactico.analizar();

        if (valido) {
            resultado.append("✅ El código es sintácticamente válido según la gramática definida.\n");
        } else {
            resultado.append(String.format("❌ Se encontraron %d error(es) sintáctico(s):\n\n",
                    sintactico.getErrores().size()));
            int contador = 1;
            for (String err : sintactico.getErrores()) {
                resultado.append(String.format("%3d. %s\n", contador++, err));
            }
        }

        resultado.append("\n═══════════════════════════════════════════════════════════\n");
        resultado.append("           ANÁLISIS COMPLETADO\n");
        resultado.append("═══════════════════════════════════════════════════════════\n");

        return resultado.toString();
    }

    /**
     * Obtiene solo los identificadores del código
     * @param codigoFuente Código a analizar
     * @return Lista de identificadores
     */
    public String obtenerIdentificadores(String codigoFuente) {
        if (codigoFuente == null || codigoFuente.trim().isEmpty()) {
            return "Error: No hay código para analizar.";
        }
        
        AnalizadorLexico analizador = new AnalizadorLexico(codigoFuente);
        List<Token> tokens = analizador.analizar();
        
        StringBuilder resultado = new StringBuilder();
        resultado.append("IDENTIFICADORES ENCONTRADOS:\n");
        resultado.append("───────────────────────────────────────────────────────────\n");
        
        int contador = 1;
        for (Token token : tokens) {
            if (token.getTipo() == TipoToken.IDENTIFICADOR) {
                resultado.append(String.format("%d. %s (Línea: %d, Columna: %d)\n", 
                                               contador++,
                                               token.getLexema(),
                                               token.getLinea(),
                                               token.getColumna()));
            }
        }
        
        if (contador == 1) {
            resultado.append("No se encontraron identificadores.\n");
        }
        
        return resultado.toString();
    }
}
