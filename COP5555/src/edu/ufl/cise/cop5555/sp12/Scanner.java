package edu.ufl.cise.cop5555.sp12;

import java.io.IOException;

import edu.ufl.cise.cop5555.sp12.TokenStream.Token;
import static edu.ufl.cise.cop5555.sp12.Kind.*;

public class Scanner {

	private enum State {
		START, GOT_EQUALS, IDENT_PART, GOT_ZERO, DIGITS, STRINGS, EOF, ONE_CHAR, TWO_CHAR, COMMENT} 	//local references to TokenStream objects for convenience
	final TokenStream stream;  //set in constructor
	private State state;
	private int index = 0; // points to the next char to process during scanning, or if none, past the end of the array
	private int begOffset = 0;

	public Scanner(TokenStream stream)  {
		this.stream = stream;
	}

	// get the next char from the token stream and update index
	private char getch() throws IOException {
		if (index < stream.inputChars.length) {
			//	System.out.println("char: " + stream.inputChars[index] + " index: "+ (index));
			return stream.inputChars[index++];
		}
		else
			return '\0';
	}

	//peak for next char
	private char peakch() throws IOException {
		if (index < stream.inputChars.length) {
			//	System.out.println("char: " + stream.inputChars[index] + " index: "+ (index));
			return stream.inputChars[index];
		}
		else
			return '\0';
	}

	//returns the next token in the input
	public Token next() throws IOException, NumberFormatException {
		state = State.START;
		begOffset = index;
		Token t = null;
		char ch = getch();
		boolean ws = false;
		if(ch == ' ')
			ws = true;
		char peak;
		boolean stringError = false;
		
		do {
			switch (state) { 
			/*in each state, check the next character. 
                   	either create a token or change state */
			case START: 
				switch (ch) {
				case '\0':case '\u001a':
					state = State.EOF;
					break; // end of file
				case ' ':case '\t':case '\n':case '\f':case '\r':					
					break; // white space
				case '*':case '+':case '-':case '.':case ';':case ',':case '(':case ')':case '[':case ']':case '{':case '}':case ':':case '/':case '|':case '&':
					state = State.ONE_CHAR; //one char
					break;
				case '!':case '<':case '>':case '=':
					state = State.TWO_CHAR;	//two char
					break;
				case '0':	//zero
					t = stream.new Token(INTEGER_LITERAL, begOffset, index);
					index--;
					break;
				case '"': //string literal
					if (peakch() != '"')
						state = State.STRINGS;
					else{
						t = stream.new Token(STRING_LITERAL, begOffset, index+1);
					}
					break;
				case '#':	//comment
					if (peakch() == '#') {
						state = State.COMMENT;
					}
					else{
						t = stream.new Token(MALFORMED_COMMENT, begOffset, index);
						index--;
					}
					break;
				default:
					if (Character.isDigit(ch)) {
						if (Character.isDigit(peakch()))
							state = State.DIGITS;
						else{
							t = stream.new Token(INTEGER_LITERAL, begOffset, index);
							index--;
						}
					} else if (Character.isJavaIdentifierStart(ch)) {
						state = State.IDENT_PART;
					} else {//HANDLE ERROR

						t = stream.new Token(ILLEGAL_CHAR, begOffset, index--);
					}
				}
				
				if(ws == true){
					begOffset = index;
					ws = false;
				}
				else
					begOffset = index - 1;
				
				if (( state != State.ONE_CHAR )&&(state != State.TWO_CHAR)&&(state != State.IDENT_PART))
					ch = getch();
				break; // end of state START

				/** Single Character Token**/
			case ONE_CHAR: 
				switch (ch) {							
				case '*':
					t = stream.new Token(TIMES, begOffset, index);
					break;
				case '+':
					t = stream.new Token(PLUS, begOffset, index);
					break;
				case '-':
					t = stream.new Token(MINUS, begOffset, index);
					break;
				case '/':
					t = stream.new Token(DIVIDE, begOffset, index);
					break;
					//SEPARATORS
				case '.':
					t = stream.new Token(DOT, begOffset, index);
					break;
				case ';':
					t = stream.new Token(SEMI, begOffset, index);
					break;
				case ',':
					t = stream.new Token(COMMA, begOffset, index);
					break;
				case '(':
					t = stream.new Token(LEFT_PAREN, begOffset, index);
					break;
				case ')':
					t = stream.new Token(RIGHT_PAREN, begOffset, index);
					break;
				case '[':
					t = stream.new Token(LEFT_SQUARE, begOffset, index);
					break;
				case ']':
					t = stream.new Token(RIGHT_SQUARE, begOffset, index);
					break;
				case '{':
					t = stream.new Token(LEFT_BRACE, begOffset, index);
					break;
				case '}':
					t = stream.new Token(RIGHT_BRACE, begOffset, index);
					break;
				case '|': //OR			
					t = stream.new Token(OR, begOffset, index);
					break;
				case '&': //AND
					t = stream.new Token(AND, begOffset, index);
					break;
				case ':':
					t = stream.new Token(COLON, begOffset, index);											
				}				
				begOffset = index--;
				ch = getch();
				break; // end of state ONE_CHAR

				/** Two Character Token**/
			case TWO_CHAR: 
				switch (ch) {	
				case '!': //NOT or NOT EQUALS
					if(peakch() == '=')
						t = stream.new Token(NOT_EQUALS, begOffset, index+1);
					else{
						t = stream.new Token(NOT, begOffset, index);
						index--;
					}
					break;		
				case '<': //LESS_THAN or AT_MOST
					if(peakch() == '=')
						t = stream.new Token(AT_MOST, begOffset, index+1);
					else{
						t = stream.new Token(LESS_THAN, begOffset, index);
						index--;
					}
					break;
				case '>': //GREATER_THAN or AT_LEAST
					if(peakch() == '=')
						t = stream.new Token(AT_LEAST, begOffset, index+1);
					else{
						t = stream.new Token(GREATER_THAN, begOffset, index);
						index--;
					}
					break;
				case '=':	//ASSIGN or EQUALS
					if(peakch() == '=')
						t = stream.new Token(EQUALS, begOffset, index+1);
					else{
						t = stream.new Token(ASSIGN, begOffset, index);
						index--;
					}
					break;
				}
				begOffset = index-1;
				ch = getch();
				break; // end of state TWO_CHAR

				/** DIGITS **/
			case DIGITS: 
				switch (peakch()) {
				case '0':case '1':case '2':case '3':case '4':case '5':case '6':case '7':case '8':case '9':
					state = State.DIGITS;
					break;
				default:
					t = stream.new Token(INTEGER_LITERAL, begOffset, index);
					index--;
					break;
				}
				ch = getch();
				break; // end of state DIGITS

				/** COMMENT **/
			case COMMENT: 
				peak = peakch();
				switch (peak) {

				case '#':
					index++;
					if(peakch() == '#'){
						state = State.START;
						index++;
						break;
					}
					else{
						index--;	
						state = State.COMMENT;
						break;
					}
				case '\0':
					t = stream.new Token(MALFORMED_COMMENT, begOffset, index);
					break;
				default:
					state = State.COMMENT;
					break;
				}
				ch = getch();
				break; // end of state COMMENTS

				/** STRINGS **/
			case STRINGS:
				switch (ch) {
				case '"': 
					if(stringError == false)
						t = stream.new Token(STRING_LITERAL, begOffset, index--);
					else{
						t = stream.new Token(MALFORMED_STRING, begOffset, index--);
						stringError = false;
					}
					break;
				case '\\':
					peak = peakch();
					if((peak == 't' )||(peak == 'n')||(peak == 'f' )
							||(peak == 'r' )||(peak == '\\' ))
						index++; 
					else{
						//t = stream.new Token(MALFORMED_STRING, begOffset, index--);
						stringError = true;
					}
					break;
				default:
					state = State.STRINGS;
					break;
				}
				ch = getch();
				break; // end of state COMMENTS


				/** INDENT PART **/
			case IDENT_PART:
				if((ch != '\0') && (Character.isJavaIdentifierPart(peakch()))){
					state = State.IDENT_PART;
				}
				else{
					//CHECK BOOLEAN LITERAL
					String word = String.valueOf(stream.inputChars, begOffset, index - begOffset);
					
					if(word.equalsIgnoreCase("false"))
					{
						t = stream.new Token(BOOLEAN_LITERAL, begOffset, index);
					}
					else if(word.equalsIgnoreCase("true")){
						t = stream.new Token(BOOLEAN_LITERAL, begOffset, index);
					}
					//CHECK KEYWORDS
					else if(word.equalsIgnoreCase("PROG")){
						t = stream.new Token(PROG, begOffset, index);
					}					
					else if(word.equalsIgnoreCase("GORP")){
						t = stream.new Token(GORP, begOffset, index);
					}
					else if(word.equalsIgnoreCase("STRING")){
						t = stream.new Token(STRING, begOffset, index);
					}
					else if(word.equalsIgnoreCase("INT")){
						t = stream.new Token(INT, begOffset, index);
					}
					else if(word.equalsIgnoreCase("BOOLEAN")){
						t = stream.new Token(BOOLEAN, begOffset, index);
					}
					else if(word.equalsIgnoreCase("MAP")){
						t = stream.new Token(MAP, begOffset, index);
					} 
					else if(word.equalsIgnoreCase("IF")){
						t = stream.new Token(IF, begOffset, index);
					}
					else if(word.equalsIgnoreCase("ELSE")){
						t = stream.new Token(ELSE, begOffset, index);
					}
					else if(word.equalsIgnoreCase("FI")){
						t = stream.new Token(FI, begOffset, index);
					}  
					else if(word.equalsIgnoreCase("DO")){
						t = stream.new Token(DO, begOffset, index);
					}
					else if(word.equalsIgnoreCase("OD")){
						t = stream.new Token(OD, begOffset, index);
					}
					else if(word.equalsIgnoreCase("PRINT")){
						t = stream.new Token(PRINT, begOffset, index);
					}
					else if(word.equalsIgnoreCase("PRINTLN")){
						t = stream.new Token(PRINTLN, begOffset, index);
					}
					//IDENTIFIER
					else{
						t = stream.new Token(IDENTIFIER, begOffset, index);
					}			
					break;
				}
				ch = getch();
				break; // end of state IDENT_PART

			case EOF: 
				t = stream.new Token(EOF, index, index);
			default:
				assert false : "should not reach here";
			}// end of switch(state)
		}   while (t == null); // loop terminates when a token is created
		return t;
	} 

	public void scan() {
		Token t;
		try {
			do {
				t = next();
		//		System.out.println(t.toString());
				stream.tokens.add(t);	

			} while (!t.kind.equals(EOF));	

		} catch (Exception e) {
			e.printStackTrace();}

		//test line number
/*		StringBuffer sb = new StringBuffer();
		for (Token x : stream.tokens) {
			if (x.kind != EOF)
				sb.append(x.getLineNumber());
		}
			System.out.println(sb.toString());*/

	}
	
/*	public static void main(String args[]){
	//	String input = ".;,()[]:{}=|&";			//illegal char?
	//	String input = "= ==!=<><=>=+-!";		//fix
	//	String input = "\"\t\n\"";		//not
	//	String input = "\"\tppgorp\n\"";	//not
	//	String input = "\"\\\\\"";
	//	String input =  "\"this is a simple\fstring literal\"";
	//	String input =  "\"abc\\";
	//	String input = "\"abc\\sfsfs\"";
	//	String input = "prog phuong if (3 + 4) int ph; fi; gorp";
		String input = "hi \n bye \r world my name is \r \n \n phuong";
		TokenStream stream = new TokenStream(input);
		Scanner s = new Scanner(stream);
		s.scan();  //create the Token array
	}*/
}


