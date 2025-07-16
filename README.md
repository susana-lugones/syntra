# Syntra

**Syntra** is a lightweight, fully-interpreted custom programming language built entirely from scratch in **Java**, designed to transform clean, minimal syntax into structured execution through a custom-built **lexer**, **parser**, **semantic analyzer**, **interpreter**, and **code generator**.

---

## **Key Features**

- **Custom Syntax**: A clean, minimalistic syntax tailored for readability and instructional clarity.  
- **Full Language Pipeline**: Complete support from source code parsing to execution.  
- **Modular Design**: Each component (Lexer, Parser, Interpreter, etc.) is independently structured with clear interfaces.  
- **Testing Infrastructure**: Extensive unit and end-to-end test coverage to ensure correctness across all stages.

---

## **Architecture**

The language is divided into the following core components:

### 1. **Lexer (Lexical Analyzer)**
- Converts raw source code into a sequence of tokens.  
- Recognizes keywords, identifiers, literals, operators, and punctuation.

### 2. **Parser**
- Builds an **Abstract Syntax Tree (AST)** from the token stream.  
- Implements a recursive descent parser supporting expressions, conditionals, loops, and function definitions.

### 3. **AST (Abstract Syntax Tree)**
- A hierarchical data structure representing the syntactic structure of the source code.  
- Acts as the backbone passed through the analyzer, interpreter, and generator.

### 4. **Semantic Analyzer**
- Performs scope checking, static type validation, and symbol resolution.  
- Detects semantic issues such as undeclared variables and improper scoping.

### 5. **Interpreter**
- Executes the program directly from the AST.  
- Manages variable bindings and function calls via the `Environment` and `Scope` modules.

### 6. **Code Generator**
- Translates AST structures into a lower-level, intermediate form (e.g., mock bytecode).  
- Simulates a compilation phase, enabling extensibility toward future virtual machine execution.

### 7. **Error Handling**
- Provides detailed diagnostics through custom `ParseException` logic.  
- Offers helpful feedback for both syntax and runtime issues, improving the debugging experience.

---

## **Testing Suite**

This language is supported by a full-featured test framework designed to validate functionality at each layer:

- **LexerTests**: Verifies correct token classification.  
- **ParserTests**: Confirms AST accuracy for a wide range of syntactic structures.  
- **AnalyzerTests**: Ensures semantic correctness, including scope and identifier validation.  
- **InterpreterTests**: Tests execution correctness and runtime behavior.  
- **GeneratorTests**: Validates translation of ASTs into intermediate output.  
- **End-to-End Tests**: Full pipeline validation from raw code to final output, ensuring integrated consistency.
