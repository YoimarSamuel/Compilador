package modelo;

/**
 * Clase que representa un token identificado por el analizador léxico
 * @author Ardila App
 */
public class Token {
    private TipoToken tipo;
    private String lexema;
    private int linea;
    private int columna;
    
    /**
     * Constructor de Token
     * @param tipo Tipo de token
     * @param lexema Texto del token
     * @param linea Línea donde se encuentra
     * @param columna Columna donde inicia
     */
    public Token(TipoToken tipo, String lexema, int linea, int columna) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.linea = linea;
        this.columna = columna;
    }

    public TipoToken getTipo() {
        return tipo;
    }

    public void setTipo(TipoToken tipo) {
        this.tipo = tipo;
    }

    public String getLexema() {
        return lexema;
    }

    public void setLexema(String lexema) {
        this.lexema = lexema;
    }

    public int getLinea() {
        return linea;
    }

    public void setLinea(int linea) {
        this.linea = linea;
    }

    public int getColumna() {
        return columna;
    }

    public void setColumna(int columna) {
        this.columna = columna;
    }

    @Override
    public String toString() {
        return String.format("Token[tipo=%s, lexema='%s', linea=%d, columna=%d]", 
                             tipo, lexema, linea, columna);
    }
    
    /**
     * Formato de salida más legible para el usuario
     */
    public String toFormattedString() {
        return String.format("<%s, '%s'> [Línea: %d, Columna: %d]", 
                             tipo, lexema, linea, columna);
    }
}
