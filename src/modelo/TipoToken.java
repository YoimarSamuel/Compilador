package modelo;

/**
 * Enumeración que define los tipos de tokens reconocidos por el analizador léxico
 * @author Ardila App
 */
public enum TipoToken {
    // Palabras reservadas
    PALABRA_RESERVADA,
    
    // Identificadores y literales
    IDENTIFICADOR,
    NUMERO_ENTERO,
    NUMERO_DECIMAL,
    CADENA,
    CARACTER,
    BOOLEANO,
    
    // Operadores aritméticos
    OPERADOR_SUMA,        // +
    OPERADOR_RESTA,       // -
    OPERADOR_MULTIPLICACION,  // *
    OPERADOR_DIVISION,    // /
    OPERADOR_MODULO,      // %
    
    // Operadores relacionales
    OPERADOR_IGUAL,       // ==
    OPERADOR_DIFERENTE,   // !=
    OPERADOR_MAYOR,       // >
    OPERADOR_MENOR,       // <
    OPERADOR_MAYOR_IGUAL, // >=
    OPERADOR_MENOR_IGUAL, // <=
    
    // Operadores lógicos
    OPERADOR_AND,         // &&
    OPERADOR_OR,          // ||
    OPERADOR_NOT,         // !
    
    // Operadores de asignación
    ASIGNACION,           // =
    ASIGNACION_SUMA,      // +=
    ASIGNACION_RESTA,     // -=
    ASIGNACION_MULTIPLICACION, // *=
    ASIGNACION_DIVISION,  // /=
    
    // Operadores de incremento/decremento
    INCREMENTO,           // ++
    DECREMENTO,           // --
    
    // Delimitadores
    PARENTESIS_IZQ,       // (
    PARENTESIS_DER,       // )
    LLAVE_IZQ,            // {
    LLAVE_DER,            // }
    CORCHETE_IZQ,         // [
    CORCHETE_DER,         // ]
    PUNTO_COMA,           // ;
    COMA,                 // ,
    PUNTO,                // .
    DOS_PUNTOS,           // :
    
    // Comentarios
    COMENTARIO_LINEA,     // //
    COMENTARIO_BLOQUE,    // /* */
    
    // Especiales
    ESPACIO_BLANCO,
    FIN_DE_ARCHIVO,
    ERROR
}
