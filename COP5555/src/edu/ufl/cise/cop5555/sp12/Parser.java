package edu.ufl.cise.cop5555.sp12;

import java.util.ArrayList;
import java.util.List;

import edu.ufl.cise.cop5555.sp12.TokenStream;
import edu.ufl.cise.cop5555.sp12.TokenStream.Token;
import edu.ufl.cise.cop5555.sp12.ast.*;
import edu.ufl.cise.cop5555.sp12.context.ContextCheckVisitor;

public class Parser {

	final TokenStream stream;
	private int tCounter = 0;
	private Token t;
	Program prog;
	public Parser(TokenStream stream)
	{ 
		this.stream = stream;
		t = stream.getToken(tCounter);
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

	void match(Kind kind) throws SyntaxException{
		if (isKind(kind))
			consume();
		else
			throw new SyntaxException(t, "Expected: " + kind);
	}

	void error(String msg) {
		System.out.println(msg);
	}

	public AST parse() throws SyntaxException{
		return Program();  //method corresponding to start symbol
	}

	/** <Program> ::= prog IDENTIFIER <Block> gorp EOF */
	private Program Program() throws SyntaxException{
		consume();
		match(Kind.PROG);
		Token ident = t;
		match(Kind.IDENTIFIER);
		Block block = block();
		match(Kind.GORP);
		if(isKind(Kind.EOF)){
			return new Program(ident, block); 
		//	System.out.println("successful parsing\n");
		}
		else
			throw new SyntaxException(t, "Expecting end of program()");	
	}

	/**	<Block> ::= (<Declaration>; | <Command>; )* */
	private Block block() throws SyntaxException{	

		List<DecOrCommand> decOrCom = new ArrayList<DecOrCommand>();
		Block b = new Block(decOrCom);
		Command c = null;
		Declaration d = null;
		if(isKind(Kind.GORP)||isKind(Kind.EOF))
			return b;

		while(!isKind(Kind.GORP) && !isKind(Kind.EOF)){
			if(isKind(Kind.INT) ||isKind(Kind.BOOLEAN) ||isKind(Kind.STRING) || isKind(Kind.MAP) ){
				d = declaration();
				decOrCom.add(d);
			}
			else if(isKind(Kind.IDENTIFIER) ||isKind(Kind.PRINT) || isKind(Kind.PRINTLN)
					||isKind(Kind.DO) ||isKind(Kind.IF)){
				c = command();		
				decOrCom.add(c);
			}
			else if(isKind(Kind.SEMI)){
				consume();
			}
			else 
				break;
		}
		b = new Block(decOrCom);
		return b;
	}

	/**	<Declaration> ::= <Type> IDENTIFIER
	<Type> ::= <SimpleType> | <CompoundType> */
	private Declaration declaration() throws SyntaxException{
		Type type = Type();
		Token ident = t;
		match(Kind.IDENTIFIER);
		match(Kind.SEMI);	
		return new Declaration(type, ident);
	}

	private Type Type() throws SyntaxException{
		Type type = null;
		if (isKind(Kind.INT) || isKind(Kind.BOOLEAN) || isKind(Kind.STRING))
			type = SimpleType();
		else if (isKind(Kind.MAP))
			type = CompoundType();
		else
			throw new SyntaxException(t, "Not a valid Type");
		return type;
	}

	/** <SimpleType> ::= int | boolean | string*/
	private SimpleType SimpleType() throws SyntaxException{
		SimpleType st = null;
		if(isKind(Kind.INT) || isKind(Kind.BOOLEAN) || isKind(Kind.STRING)){
			st = new SimpleType(t.kind);
			consume();
		}
		else
			throw new SyntaxException(t, "Not a valid SimpleType");

		return st;			
	}

	/** <CompoundType> ::= map [ <SimpleType>, <Type>]	*/
	private CompoundType CompoundType() throws SyntaxException{
		CompoundType ct = null;
		match(Kind.MAP);
		match(Kind.LEFT_SQUARE);

		SimpleType keyType = SimpleType();
		match(Kind.COMMA);
		Type valType = Type();

		match(Kind.RIGHT_SQUARE);
		ct = new CompoundType(keyType, valType);
		return ct;
	}

	/**	<Command> ::= <LValue> = <Expression> | <LValue> = <PairList> | print <Expression>
	| println <Expression>	| do (<Expression>) <Block> od	| do (<LValue> : [ IDENTIFIER , IDENTIFIER] ) <Block> od
	| if (<Expression> ) <Block> fi	| if (<Expression>) <Block> else <Block> fi | empty*/
	private Command command() throws SyntaxException{
		Command c = null;

		switch(t.kind){
		case IDENTIFIER:
			LValue lv = LValue();	// <LValue> = <Expression> | <LValue> = <PairList> 
			match(Kind.ASSIGN);
			if(isKind(Kind.LEFT_BRACE))
				c = new AssignPairListCommand(lv, Pairlist());
			else
				c = new AssignExprCommand(lv, Expression());
			break;
		case PRINT: //print <Expression>
			consume();
			c = new PrintCommand(Expression()); 
			break;
		case PRINTLN: //println <Expression>
			consume();
			c = new PrintlnCommand(Expression()); 
			break;
		case DO: 	//do (<Expression>) <Block> od	| do <LValue> : [ IDENTIFIER , IDENTIFIER] <Block> od
			consume();
			if(isKind(Kind.LEFT_PAREN)){
				consume();
				Expression e = Expression();
				match(Kind.RIGHT_PAREN);
				Block b = block();
				match(Kind.OD);

				c = new DoCommand(e, b);
			}
			else if(isKind(Kind.IDENTIFIER)) {// <LValue> : [ IDENTIFIER , IDENTIFIER]  <Block> od
				LValue v = LValue();
				match(Kind.COLON);
				match(Kind.LEFT_SQUARE);

				Token key = t;
				match(Kind.IDENTIFIER);
				match(Kind.COMMA);

				Token val = t;
				match(Kind.IDENTIFIER);
				match(Kind.RIGHT_SQUARE);
				Block b = block();
				match(Kind.OD);

				c = new DoEachCommand(v, key, val, b);
			}
			else
				throw new SyntaxException(t, "Not a valid Do statement");
			break;	
		case IF:
			//if (<Expression> ) <Block> fi	| if (<Expression>) <Block> else <Block> fi
			consume();
			match(Kind.LEFT_PAREN);			
			Expression expr = Expression();
			match(Kind.RIGHT_PAREN);
			Block ifBlock = block();
			if(!isKind(Kind.FI) && !isKind(Kind.ELSE)){
				throw new SyntaxException(t, "Expected FI or ELSE");	
			}
			if(isKind(Kind.FI)){
				consume();
				c = new IfCommand(expr, ifBlock);
			}
			else if(isKind(Kind.ELSE)){
				consume();
				Block elseBlock = block();
				match(Kind.FI);	
				c = new IfElseCommand(expr, ifBlock, elseBlock);
			}
			break;
		}
		match(Kind.SEMI);
		return c;
	}

	//<LValue> ::= IDENTIFIER | IDENTIFIER [ <Expression> ]
	private LValue LValue() throws SyntaxException{
		LValue lv = null;
		Token ident = t;
		consume();
		if(isKind(Kind.LEFT_SQUARE)){
			consume(); //consumer [
			lv = new ExprLValue(ident, Expression());
			match(Kind.RIGHT_SQUARE);
		} 
		else
			lv = new SimpleLValue(ident);		
		return lv;
	}
	//<PairList> ::= { <Pair> ( , <Pair> )* } | { }
	private PairList Pairlist() throws SyntaxException{
		consume();
		List<Pair> pairs = new ArrayList<Pair>();
		if(isKind(Kind.LEFT_SQUARE)){
			pairs.add(Pair());		
		}
		while(isKind(Kind.COMMA)){
			consume();
			pairs.add(Pair());
		}
		match(Kind.RIGHT_BRACE);
		return new PairList(pairs);	
	}

	// <Pair> ::= [ <Expression> , <Expression> ]
	private Pair Pair() throws SyntaxException{
		Expression e0 = null;
		Expression e1 = null;
		match(Kind.LEFT_SQUARE);
		e0 = Expression();
		match(Kind.COMMA);
		e1 = Expression();
		match(Kind.RIGHT_SQUARE);

		return new Pair(e0, e1);
	}

	//<Expression> ::= <Term> (<RelOp> <Term>)*
	private Expression Expression() throws SyntaxException{
		Expression e0 = null;
		Expression e1 = null;
		e0 = Term();
		while(RelOp()){
			Kind op = t.kind;
			consume();
			e1 = Term();
			e0 = new BinaryOpExpression(e0, op, e1);
		}
		return e0;
	}

	//<Term> ::= <Elem> (<WeakOp> <Elem>)*
	private Expression Term() throws SyntaxException{
		Expression e0 = null;
		Expression e1 = null;
		e0 = Elem();
		while(WeakOp()){
			Kind op = t.kind;
			consume();
			e1 = Elem();
			e0 = new BinaryOpExpression(e0, op, e1);
		}	
		return e0;
	}

	//	<Elem> ::= <Factor> ( <StrongOp> <Factor )*
	private Expression Elem() throws SyntaxException {
		Expression e0 = null;
		Expression e1 = null;
		e0 = Factor();
		while(StrongOp()){
			Kind op = t.kind;
			consume();
			e1 = Factor();
			e0 = new BinaryOpExpression(e0, op, e1);
		}
		return e0;
	}


	/* <Factor>::= <LValue>| INTEGER_LITERAL | BOOLEAN_LITERAL | STRING_LITERAL
	| ( <Expression> ) | ! <Factor> | -<Factor> | <PairList> */
	public Expression Factor() throws SyntaxException{
				
		Expression e = null;		
		switch (t.kind){
		case IDENTIFIER: 
			e = new LValueExpression (LValue());
			break;
		case INTEGER_LITERAL:
			e = new IntegerLiteralExpression(t);
			consume();
			break;
		case BOOLEAN_LITERAL: 
			e = new BooleanLiteralExpression(t); 
			consume();
			break;
		case STRING_LITERAL: 
			e = new StringLiteralExpression(t);
			consume();
			break;		
		default:
			if(isKind(Kind.LEFT_PAREN)) { // ( <Expression> ) 
				consume();
				e = Expression();
				match(Kind.RIGHT_PAREN);			
			}
			else if (isKind(Kind.NOT) || isKind(Kind.MINUS)){
				Kind k = t.kind;
					consume();
				
				e = new UnaryOpExpression(k, Factor());
			}
			else{
				throw new SyntaxException(t, "Expecting factor");
			}
		}
		return e;		
	}

	//	<RelOp> ::= OR | AND | EQUALS | NOT_EQUALS | LESS_THAN | GREATER_THAN | AT_MOST | AT_LEAST
	public boolean RelOp(){	
		switch (t.kind) 
		{
		case OR: case AND: case EQUALS: case NOT_EQUALS: 
		case LESS_THAN: case GREATER_THAN: case AT_MOST: case AT_LEAST:
			return true;
		}
		return false;
	}
	public boolean WeakOp(){	//	<WeakOp> ::= PLUS | MINUS
		switch (t.kind) 
		{
		case PLUS: case MINUS:
			return true;
		}
		return false;
	}
	public boolean StrongOp(){	//<StrongOp> ::= TIMES | DIVIDE
		switch (t.kind) 
		{
		case TIMES: case DIVIDE:
			return true;
		}

		return false;
	}
	
//	 TESTS
	public static void main (String args[]) throws SyntaxException{
		//TokenStream stream = new TokenStream("prog phuong map[int,map[int, boolean]] ph; gorp");
		//TokenStream stream = new TokenStream("prog phuong map[int,map[int, boolean]] ph; int two; gorp");
		//TokenStream stream = new TokenStream("prog phuong if (3 < 4) ph = a; fi; gorp");
		//TokenStream stream = new TokenStream("prog phuong int ph; do (ph + 4) int hd; od; gorp");
		//TokenStream stream = new TokenStream("prog phuong ph = {[2, 4], [3, 5]}; gorp");
		//TokenStream stream = new TokenStream("prog phuong println \"phuong\"; gorp");
		//TokenStream stream = new TokenStream("prog phuong if(6 > 4) ph = 5 + 6; else ph = 2*4; fi; gorp");
		//TokenStream stream = new TokenStream("prog Test1 boolean x; gorp");
		//TokenStream stream = new TokenStream("prog Test1 int x; gorp");
		//TokenStream stream = new TokenStream("prog Test1 map[int,string] y; gorp");
		//TokenStream stream = new TokenStream("prog Test1 map[int,map[string,boolean]] m; gorp");
		//TokenStream stream = new TokenStream("prog Test1 map[int,map[string,boolean]] ; gorp");
		//TokenStream stream = new TokenStream("prog Test1 m = -\"a\"; m2 = -!\"a\"; m3 = !-\"a\"; m4 = (!-\"a\"); gorp");
		//TokenStream stream = new TokenStream("prog Test1 if ((a + b - c * d / e | \"a\" < true > false >= x <= y - -a != k & f)) a = true; else y=s; fi;  gorp");
		//TokenStream stream = new TokenStream("prog Test1 do (a + b - c * d / e | \"a\" < true > false >= x <= y - -a != k & f) int x; string y; boolean z; map[int, map[int, map[int, string]]] m1; od; gorp");
		//TokenStream stream = new TokenStream("prog Test1 do (a + b - c * d / e | \"a\" < true > false >= x <= y - -a != k & f) int x; string y; boolean z; map[int, map[int, map[int, string]]] m1; ab = sc; od; gorp");
		//TokenStream stream = new TokenStream("prog Test1 int x; boolean y; map[int, map[boolean, map[string, int]]] m1; do (a + b - c * d / e | \"a\" < true > false >= x <= y - -a != k & f) int x; string y; boolean z; map[int, map[int, map[int, string]]] m1; ab = sc; od; gorp");
		//TokenStream stream = new TokenStream("prog Test1 if (a + b - c * d / e | \"a\" < true > false >= x <= y - -a != k & f) ab = true ; ; ; fi; gorp");
		//TokenStream stream = new TokenStream("prog Test1 ; ; gorp");
		//TokenStream stream = new TokenStream("prog Test1 if (a + b - c * d / e | \"a\" < true > false >= x <= y - -a != k & f) int x; string y; boolean z; map[int, map[int, map[int, string]]] m1; ab = sc; ;fi;  gorp");
		//TokenStream stream = new TokenStream("prog Test1 if ((a + b - c * d / e | \"a\" < true > false >= x <= y - -a != k & f)) a = true; else y=s; fi;  gorp");
		//TokenStream stream = new TokenStream("prog Test1 if (a + b - c * d / e | \"a\" < true > false >= x <= y - -a != k & f) int x; string y; boolean z; map[int, map[int, map[int, string]]] m1; ab = sc;  fi;  gorp");
		// TokenStream stream = new TokenStream("prog Test1 do (a + b - c * d / e | \"a\" < true > false >= x <= y - -a != k & f) int x; string y; boolean z; map[int, map[int, map[int, string]]] m1; od; gorp");
		 //TokenStream stream = new TokenStream("prog Test1 if (a + b - c * d / e | \"a\" < true > false >= x <= y - -a != k & f) ab = true ; ; ; fi; gorp");
		// TokenStream stream = new TokenStream("prog Test1 m = -\"a\"; m2 = -!\"a\"; m3 = !-\"a\"; m4 = (!-\"a\"); gorp");
		//String input = "prog Test1 int x; x = 4; gorp";
		//String input = "prog Test map[int,string] a; a= {[1,\"hi\"]}; gorp";
		//String input = "prog phuong int ph; do (false) int hd; od; gorp";
	/*	String input = "prog phuong " +
				"map[int, int] x;" +
				"int a; " +
				"a = 5; " +
				"int b; " +
				"string g;" +
				"boolean c;" +
				"c = false;"+
				"map[int, map[int, int]] f;" +
				"map[int, int] l;" +
				"l = {[3,6]};" +
				"f[4] = {[5,l]};"+				
				"b = 3;"+
				"map[int, boolean] p;" +
				"if (a > b) int s; " +
					"do x:[a,b] " +
						"boolean d; " +
					"od;" +
				" else c = true; fi;" +
				"p = {[a,false],[b,false]};" +
				"g = 1 + \"hi\";" +
				"if(-a < 3) fi;"+
				"gorp";
		
		String input ="prog Test1 " +
				"int x; " +
				"int z; " +
				"int y; " +
				"x = -32;" +
				"map[boolean,map[string,map[boolean,int]]] fecker;" +
				"map[int,map[boolean,map[string,map[boolean,int]]]] facker; " +
				"facker[x] = fecker;" +
				"string s0;"+
				"string s1;"+
				"s0 = \"test string\";"+
				"s1 = s0+x*3;"+
				"map[boolean, int] m2;"+
				"m2[true] = -32;" +
				"map[int, int] m;"+
				"m = {[313,13], [123, 123]};"+
				"boolean b0;"+
				"boolean b1;"+
				"b1 = !b0 & (x == y);" +
				"b1 = !b0 & (s0 == s1);" +
				"b1 = !b0 & (x >= y);" +
				"b1 = !b0 & (x <= y);" +
				"b1 = !b0 & (b0 <= b1);" +
				"if(true)" +
					"boolean wawa;" +
					"##w = x;##" +
				"else\n" +
					"int wawa;"+
					"wawa = x;"+
				"fi;" +
				"b0 = true; " +
				"do(true) " +
					"int z; " +
					"int f;" +
					"boolean facker;" +
					"do(false)" +
						"int w; " +
						"w = 42;" +
					"od;"+
					"z = x;" +
				"od;" +
				"do(true) " +
					"int z; " +
					"int f;" +
					"do(true)" +
						"map[int,map[boolean,map[string,map[boolean,int]]]] w; " +
						"if(false)" +
							"int w; " +
							"if(true)" +
								"int w; " +
								"boolean x;" +
								"w = (32+32)*64; " +	
								"map[boolean, int] m1;"+
								"m1 = {[b0, 32], [b1, 33]};" +
								"m1 = m1*m1;" +
								"m1 = m1-m1;" +
								"m1[x] = 32;" +
							"fi;" +
						"fi;" +
						"do facker:[x,fecker]" +
							"int w;" +
						"od;" +
						"w = facker;" +
					"od;"+
					"z = x;" +
				"od;" +
				"x = y;"+
			"gorp";*/
		//String input = "prog Test1 int x; boolean y; string s; map [int, string] m; x = s/m; gorp";
		//String input = "prog Test1 int x; map[int,int] y; x = x * y; gorp";
		//String input = "prog Test1 int x; map[int,int] y; y = y - x; gorp";
		String input = "prog Test1 map[int, string] x; x = {[10, \"string\"]} ; gorp"; //fail
		//String input = "prog Test1 boolean x; boolean y; do(x + y) od; gorp";
		//String input = "prog Test1 map [int, int] m; int x1; do m :[x1, x2] od; gorp";
		//String input = "prog Test1 map[int, string] x; map[int, string] y; int z; do x : [z,y] od; gorp";
		System.out.println(input);
		TokenStream stream = new TokenStream(input);
		Scanner scanner = new Scanner(stream);
		scanner.scan();
		Parser parser = new Parser(stream);
		AST ast = parser.parse();  
	
		ASTVisitor contextChecker = new ContextCheckVisitor();
		try {
			ast.visit(contextChecker,null);
			System.out.println("No Context Errors");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	/*	try {
			ToStringVisitor pv = new ToStringVisitor();
			ast.visit(pv,"");
			String s = pv.getString(); 
			System.out.println(s);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} */

		//	SyntaxException e = parser.parse();
		//	System.out.println(e.getMessage());
		
	}

}
