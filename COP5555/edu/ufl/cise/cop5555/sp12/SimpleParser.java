package edu.ufl.cise.cop5555.sp12;

import edu.ufl.cise.cop5555.sp12.TokenStream;
import edu.ufl.cise.cop5555.sp12.TokenStream.Token;


public class SimpleParser {

	final TokenStream stream;
	private int tCounter = 0;
	private Token t;

	public SimpleParser(TokenStream stream)
	{ 
		this.stream = stream;
		t = stream.getToken(tCounter);
	}

	public SyntaxException parse() {
		try{
			Program();  //method corresponding to start symbol
			return null;
		}   catch (SyntaxException e){return e;}
	}

	//Get Next Token
	public void consume() {
		t = stream.getToken(tCounter++);
	}
	public boolean peakToken(Kind kind) {
		return stream.getToken(tCounter+1).kind == kind;
	}

	//Check the Kind
	boolean isKind(Kind kind) {
		return kind == t.kind;
	}

	void match(Kind kind) {
		if (isKind(kind)) {
			consume();
		} else error("expected " + kind);
	}

	void error(String msg) {
		System.out.println(msg);
	}


	/** <Program> ::= prog IDENTIFIER <Block> gorp EOF */
	private void Program() throws SyntaxException{
		consume();
		if (isKind(Kind.PROG)){
			consume();
			if(isKind(Kind.IDENTIFIER)){
				consume();
				block();
				if(isKind(Kind.GORP)){
					consume();
					if(isKind(Kind.EOF))
						System.out.println("successful parsing");
					else
						throw new SyntaxException(t, "Expecting end of program()");
				}
				else
					throw new SyntaxException(t, "Expecting end of program()");
			}
			else
				throw new SyntaxException(t, "Expecting IDENT in program()");			
		}
		else
			throw new SyntaxException(t, "Expecting PROG to begin program()");
	}

	/**	<Block> ::= (<Declaration>; | <Command>; )* */
	private void block() throws SyntaxException{		
		//base case: check if null
		if(isKind(Kind.GORP)||isKind(Kind.EOF))
			return;
		//check if declaration
		while(!isKind(Kind.GORP) && !isKind(Kind.EOF)){
			if(isKind(Kind.INT) ||isKind(Kind.BOOLEAN) ||isKind(Kind.STRING) || isKind(Kind.MAP) ){
				declaration();
			}
			else if(isKind(Kind.SEMI) ||isKind(Kind.IDENTIFIER) ||isKind(Kind.PRINT) || isKind(Kind.PRINTLN)
					||isKind(Kind.DO) ||isKind(Kind.IF)){
				command();				
			}
			else 
				break;
		}
	}

	/**	<Declaration> ::= <Type> IDENTIFIER
	<Type> ::= <SimpleType> | <CompoundType> */
	private void declaration() throws SyntaxException{
		if(SimpleType() || CompoundType()){	//check type
			if(isKind(Kind.IDENTIFIER)){
				consume();
				if(isKind(Kind.SEMI))
					consume();
				else
					throw new SyntaxException(t, "Expected semicolon following identifier.");
			}
			else 
				throw new SyntaxException(t, "Expected identifier following declaration.");		
		} else
			throw new SyntaxException(t, "Invalid declaration");
	}

	/** <SimpleType> ::= int | boolean | string*/
	private boolean SimpleType(){
		switch(t.kind){
		case INT: case BOOLEAN: case STRING: 
			consume();
			return true;
		}
		return false;
	}

	/** <CompoundType> ::= map [ <SimpleType>, <Type>]	*/
	private boolean CompoundType(){
		if(isKind(Kind.MAP)){
			consume();
			if(isKind(Kind.LEFT_SQUARE)){
				consume();
				if(SimpleType()){
					if(isKind(Kind.COMMA)){
						consume();
						if(SimpleType() || CompoundType()){
							if(isKind(Kind.RIGHT_SQUARE)){
								consume();
								return true;
							}
						}							
					}
				}					
			}			
		}
		return false;
	}

	/**	<Command> ::= <LValue> = <Expression> | <LValue> = <PairList> | print <Expression>
	| println <Expression>	| do (<Expression>) <Block> od	| do (<LValue> : [ IDENTIFIER , IDENTIFIER] ) <Block> od
	| if (<Expression> ) <Block> fi	| if (<Expression>) <Block> else <Block> fi | empty*/
	private void command() throws SyntaxException{
		switch(t.kind){
		case SEMI:
			break;
		case IDENTIFIER:
			LValue();	// <LValue> = <Expression> | <LValue> = <PairList> 
			if(isKind(Kind.ASSIGN)){
				consume();
				if(isKind(Kind.LEFT_BRACE))
					Pairlist();
				else
					Expression();
			}else
				throw new SyntaxException(t, "Expected assign following identifier.");					
			break;
		case PRINT: case PRINTLN: //print <Expression> | println <Expression>
			consume();
			Expression(); 
			break;
		case DO: 	//do (<Expression>) <Block> od	| do <LValue> : [ IDENTIFIER , IDENTIFIER] <Block> od
			consume();
			if(isKind(Kind.LEFT_PAREN)){
				consume();
				//check if it is an expression
				Expression();
				if(isKind(Kind.RIGHT_PAREN)){
					consume();
					block();
					if(isKind(Kind.OD))
						consume();
					else
						throw new SyntaxException(t, "Expected OD");
				}else throw new SyntaxException(t, "Expected )");
			}
			else if(isKind(Kind.IDENTIFIER)) {// <LValue> : [ IDENTIFIER , IDENTIFIER]  
				LValue();
				if(isKind(Kind.COLON)){
					consume();
					if(isKind(Kind.LEFT_SQUARE)){
						consume();
						if(isKind(Kind.IDENTIFIER)){
							consume();
							if(isKind(Kind.COMMA)){
								consume();
								if(isKind(Kind.IDENTIFIER)){
									consume();
									if(isKind(Kind.RIGHT_SQUARE)){
										consume();
										block();
										if(isKind(Kind.OD))
											consume();
										else
											throw new SyntaxException(t, "Expected OD");
									}else throw new SyntaxException(t, "Expected ]");
								}else throw new SyntaxException(t, "Expected identifier");
							}else throw new SyntaxException(t, "Expected ,");
						} else throw new SyntaxException(t, "Expected identifier");
					} else throw new SyntaxException(t, "Expected [");
				} else throw new SyntaxException(t, "Expected :");	
			}
			else
				throw new SyntaxException(t, "Not a valid Do statement");
			break;	
		case IF:
			//if (<Expression> ) <Block> fi	| if (<Expression>) <Block> else <Block> fi
			consume();
			if(isKind(Kind.LEFT_PAREN)){
				consume();
				Expression();
				if(isKind(Kind.RIGHT_PAREN)){
					consume();
					block();
					if(!isKind(Kind.FI) && !isKind(Kind.ELSE)){
						throw new SyntaxException(t, "Expected FI or ELSE");	
					}
					if(isKind(Kind.FI))
						consume();
					else if(isKind(Kind.ELSE)){
						consume();
						block();
						if(isKind(Kind.FI))
							consume();
						else
							throw new SyntaxException(t, "Expected FI");						
					}
				}else throw new SyntaxException(t, "Expected )");	
			}else throw new SyntaxException(t, "Not a valid if statement, expect (");
			break;
		}
		if(isKind(Kind.SEMI))
			consume();
		else
			throw new SyntaxException(t, "Expected ;");
	}

	//<LValue> ::= IDENTIFIER | IDENTIFIER [ <Expression> ]
	private void LValue() throws SyntaxException{
		consume();
		if(isKind(Kind.LEFT_SQUARE)){
			consume(); //consumer [
			Expression();
			if(isKind(Kind.RIGHT_SQUARE)){
				consume();
			}else throw new SyntaxException(t, "Expected ]");
		} 
	}
	//<PairList> ::= { <Pair> ( , <Pair> )* } | { }
	private void Pairlist() throws SyntaxException{
		consume();
		if(isKind(Kind.LEFT_SQUARE))
			Pair();			
		while(isKind(Kind.COMMA)){
			consume();
			Pair();
		}
		if(isKind(Kind.RIGHT_BRACE))
			consume();		
		else
			throw new SyntaxException(t, "Expected }");
	}

	// <Pair> ::= [ <Expression> , <Expression> ]
	private void Pair()  throws SyntaxException{
		if(isKind(Kind.LEFT_SQUARE)){
			consume();
			Expression();
			if(isKind(Kind.COMMA)){
				consume();
				Expression();
				if(isKind(Kind.RIGHT_SQUARE))
					consume();
				else
					throw new SyntaxException(t, "Expected ]");
			}else throw new SyntaxException(t, "Expected ,");
		}else throw new SyntaxException(t, "Expected [");		
	}

	//<Expression> ::= <Term> (<RelOp> <Term>)*
	private void Expression() throws SyntaxException{
		Term();
		while(RelOp()){
			Term();
		}
	}

	//<Term> ::= <Elem> (<WeakOp> <Elem>)*
	private void Term() throws SyntaxException{
		Elem();
		while(WeakOp()){
			Elem();
		}		
	}

	//	<Elem> ::= <Factor> ( <StrongOp> <Factor )*
	private void Elem() throws SyntaxException {
		Factor();
		while(StrongOp()){
			Factor();
		}
	}


	/* <Factor>::= <LValue>| INTEGER_LITERAL | BOOLEAN_LITERAL | STRING_LITERAL
	| ( <Expression> ) | ! <Factor> | -<Factor> | <PairList> */
	public void Factor() throws SyntaxException{
		switch (t.kind){
		case IDENTIFIER: 
			LValue();
			break;
		case INTEGER_LITERAL: case BOOLEAN_LITERAL: case STRING_LITERAL: 
			consume();
			break;
		default:
			if(isKind(Kind.LEFT_PAREN)) { // ( <Expression> ) 
				consume();
				Expression();
				match(Kind.RIGHT_PAREN);
				break;
			}
			else if (isKind(Kind.NOT) || isKind(Kind.MINUS)){
				consume();
				Factor();
			}
			else{
				throw new SyntaxException(t, "Expecting factor");
			}
		}
	}

	//	<RelOp> ::= OR | AND | EQUALS | NOT_EQUALS | LESS_THAN | GREATER_THAN | AT_MOST | AT_LEAST
	public boolean RelOp(){	
		switch (t.kind) 
		{
		case OR: case AND: case EQUALS: case NOT_EQUALS: 
		case LESS_THAN: case GREATER_THAN: case AT_MOST: case AT_LEAST:
			consume(); 
			return true;
		}
		return false;
	}
	public boolean WeakOp(){	//	<WeakOp> ::= PLUS | MINUS
		switch (t.kind) 
		{
		case PLUS: case MINUS:
			consume(); 
			return true;
		}
		return false;
	}
	public boolean StrongOp(){	//<StrongOp> ::= TIMES | DIVIDE
		switch (t.kind) 
		{
		case TIMES: case DIVIDE:
			consume(); 
			return true;
		}
		
		return false;
	}
	public static void main (String args[]){
		//TokenStream stream = new TokenStream("prog phuong map[int,map[int, boolean]] ph; gorp");
		//TokenStream stream = new TokenStream("prog phuong map[int,map[int, boolean]] ph; int two; gorp");
		//TokenStream stream = new TokenStream("prog phuong if (3 < 4) int ph; fi; gorp");
		//TokenStream stream = new TokenStream("prog phuong do (ph + 4) int hd; od; gorp");
		//TokenStream stream = new TokenStream("prog phuong ph = {[2, 4], [3, 5]}; gorp");
		//TokenStream stream = new TokenStream("prog phuong println \"phuong\"; gorp");
	//	TokenStream stream = new TokenStream("prog phuong if(6 > 4) ph = 5 + 6; else ph = 2*4; fi; gorp");

		TokenStream stream = new TokenStream( "prog phuong if (3 < 4) ph = 4; fi; gorp");
		Scanner scanner = new Scanner(stream);
		scanner.scan();
		SimpleParser parser = new SimpleParser(stream);
		SyntaxException e = parser.parse();
		System.out.println(e.getMessage());
		
	}
}
