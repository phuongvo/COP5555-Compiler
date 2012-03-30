package edu.ufl.cise.cop5555.sp12.context;


import java.util.List;

import edu.ufl.cise.cop5555.sp12.ast.Type;
import edu.ufl.cise.cop5555.sp12.Kind;
import edu.ufl.cise.cop5555.sp12.ast.AST;
import edu.ufl.cise.cop5555.sp12.ast.ASTVisitor;
import edu.ufl.cise.cop5555.sp12.ast.AssignExprCommand;
import edu.ufl.cise.cop5555.sp12.ast.AssignPairListCommand;
import edu.ufl.cise.cop5555.sp12.ast.BinaryOpExpression;
import edu.ufl.cise.cop5555.sp12.ast.Block;
import edu.ufl.cise.cop5555.sp12.ast.BooleanLiteralExpression;
import edu.ufl.cise.cop5555.sp12.ast.CompoundType;
import edu.ufl.cise.cop5555.sp12.ast.DecOrCommand;
import edu.ufl.cise.cop5555.sp12.ast.Declaration;
import edu.ufl.cise.cop5555.sp12.ast.DoCommand;
import edu.ufl.cise.cop5555.sp12.ast.DoEachCommand;
import edu.ufl.cise.cop5555.sp12.ast.ExprLValue;
import edu.ufl.cise.cop5555.sp12.ast.IfCommand;
import edu.ufl.cise.cop5555.sp12.ast.IfElseCommand;
import edu.ufl.cise.cop5555.sp12.ast.IntegerLiteralExpression;
import edu.ufl.cise.cop5555.sp12.ast.LValueExpression;
import edu.ufl.cise.cop5555.sp12.ast.Pair;
import edu.ufl.cise.cop5555.sp12.ast.PairList;
import edu.ufl.cise.cop5555.sp12.ast.PrintCommand;
import edu.ufl.cise.cop5555.sp12.ast.PrintlnCommand;
import edu.ufl.cise.cop5555.sp12.ast.Program;
import edu.ufl.cise.cop5555.sp12.ast.SimpleLValue;
import edu.ufl.cise.cop5555.sp12.ast.SimpleType;
import edu.ufl.cise.cop5555.sp12.ast.StringLiteralExpression;
import edu.ufl.cise.cop5555.sp12.ast.UnaryOpExpression;

public class ContextCheckVisitor implements ASTVisitor{

	private SymbolTable symbolTable = new SymbolTable();
	String programName = new String();

	//Check if condition met, else throw Context Exception
	private void check(boolean condition, AST ast, String string) throws ContextException {
		if(!condition){
			throw new ContextException(ast, string);
		}
	}

	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		//The IDENTIFIER is a program name and cannot be used as a variable name
		programName = program.ident.getText();
		program.block.visit(this,arg);
		return null;
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws Exception {
		//Each <Block> delineates a scope.
		symbolTable.enterScope();		
		for (DecOrCommand cd : block.decOrCommands) {
			cd.visit(this, arg);
		}
		symbolTable.exitScope();

		return null;
	}

	@Override
	public Object visitDeclaration(Declaration declaration, Object arg) throws Exception {		
		String ident = declaration.ident.getText();
		//condition programName != ident
		check(!ident.equals(programName), declaration, "Program name and cannot be used as a variable name.");
		//Condition: IDENTIFIER doesn't exist in current scope. Insert name and declaration in symbol table.
		check(symbolTable.insert(ident, declaration), declaration, "Identifier already declared in current scope.");

		declaration.type.visit(this, arg);

		return null;
	}

	@Override
	public Object visitSimpleType(SimpleType simpleType, Object arg) throws Exception {	
		//<SimpleType>.type := int or boolean or string
		return simpleType;
	}

	@Override
	public Object visitCompoundType(CompoundType compoundType, Object arg) throws Exception {
		//<CompoundType>.type := (keyType, valType) where keyType = <SimpleType>.type and valType = <Type>.type
		compoundType.keyType.visit(this,arg); 
		compoundType.valType.visit(this,arg);

		return compoundType;
	}

	@Override
	public Object visitAssignExprCommand(AssignExprCommand assignExprCommand, Object arg) throws Exception {
		//Condition: <LValue>.type == <Expression>.type
		Type lhsType = (Type) assignExprCommand.lValue.visit(this,arg);
		Type exprType = (Type) assignExprCommand.expression.visit(this,arg);

		check(lhsType.equals(exprType), assignExprCommand, "incompatible types in assignment");
		return null;
	}

	@Override
	public Object visitAssignPairListCommand(AssignPairListCommand assignPairListCommand, Object arg) throws Exception {
		/*Condition:	PairList is empty or 
		 * let<LValue>.type = (keyType,valType) and
		<PairList>.type = (pairKeyType,pairValType) in
		keyType == pairKeyType &&
		valType == pairValType*/

		List<Pair> pairList = assignPairListCommand.pairList.pairs;

		if(!pairList.isEmpty()) {		

			assignPairListCommand.pairList.visit(this,arg); //visit pairList to make sure all pairs have the same type


			Type type = (Type) assignPairListCommand.lValue.visit(this, arg);			
			Type pairKeyType = (Type) pairList.get(0).expression0.visit(this,arg);
			Type pairValType = (Type) pairList.get(0).expression1.visit(this,arg);

			check(type.type == Kind.MAP, assignPairListCommand, "incompatible types in pairlist assignment");

			CompoundType lValueType = (CompoundType) type;
			Type keyType = lValueType.keyType;
			Type valType = lValueType.valType;
			check(keyType.equals(pairKeyType) && valType.equals(pairValType), 
					assignPairListCommand, "incompatible types in pairlist assignment");
			
		}
		return null;
	}

	@Override
	public Object visitPrintCommand(PrintCommand printCommand, Object arg) throws Exception {
		printCommand.expression.visit(this,arg); 
		return null;
	}

	@Override
	public Object visitPrintlnCommand(PrintlnCommand printlnCommand, Object arg)
			throws Exception {
		printlnCommand.expression.visit(this,arg); 
		return null;
	}

	@Override
	public Object visitDoCommand(DoCommand doCommand, Object arg)
			throws Exception {
		//Condition: <Expression>.type == boolean
		Type exprType = (Type) doCommand.expression.visit(this, arg);
		check(exprType.type.equals(Kind.BOOLEAN), doCommand, "incompatible type in do Command");

		doCommand.block.visit(this,arg); 
		return null;
	}

	@Override
	public Object visitDoEachCommand(DoEachCommand doEachCommand, Object arg)
			throws Exception {
		/*Condition:
			let<LValue>.type = (keyType,valType)
			in
			Identifier0t.type == keyType &&
			Identifier1.type == valType*/

		CompoundType lvType = (CompoundType) doEachCommand.lValue.visit(this,arg); 

		String ident0 = doEachCommand.key.getText();
		String ident1 = doEachCommand.val.getText();		
		Declaration dec0 = symbolTable.lookup(ident0);
		Declaration dec1 = symbolTable.lookup(ident1);
		boolean dec0existInScope = symbolTable.existInScope(ident0);
		boolean dec1existInScope = symbolTable.existInScope(ident0);
		check(dec0 != null, doEachCommand, "variable " + ident0 + " does not exist in symbol table");
		check(dec0existInScope, doEachCommand, "variable " + ident0 + " is undefined in current scope");	
		check(dec1 != null, doEachCommand, "variable " + ident1 + " does not exist in symbol table");
		check(dec1existInScope, doEachCommand, "variable " + ident1 + " is undefined in current scope");

		Type ident0Type = dec0.type;
		Type ident1Type = dec1.type;		
		Type keyType = lvType.keyType;
		Type valType = lvType.valType;	

		check(ident0Type.equals(keyType), doEachCommand, "incompatible keyType in do each Command");
		check(ident1Type.equals(valType), doEachCommand, "incompatible valType in do each Command");

		doEachCommand.block.visit(this,arg); 
		return null;
	}

	@Override
	public Object visitIfCommand(IfCommand ifCommand, Object arg)
			throws Exception {
		//<Expression>.type == boolean
		Type exprType = (Type) ifCommand.expression.visit(this,arg); 
		check(exprType.type.equals(Kind.BOOLEAN), ifCommand, "If command is not a boolean type");

		ifCommand.block.visit(this,arg); 	
		return null;
	}

	@Override
	public Object visitIfElseCommand(IfElseCommand ifElseCommand, Object arg)
			throws Exception {
		//<Expression>.type == boolean
		Type exprType = (Type) ifElseCommand.expression.visit(this,arg); 
		check(exprType.type.equals(Kind.BOOLEAN), ifElseCommand, "If Else command is not a boolean type");

		ifElseCommand.ifBlock.visit(this,arg); 		
		ifElseCommand.elseBlock.visit(this,arg); 
		return null;
	}

	@Override
	public Object visitSimpleLValue(SimpleLValue simpleLValue, Object arg) throws Exception {
		//Condition: IDENTIFIER exists in symbol table and defined in current scope.
		String ident = simpleLValue.identifier.getText();
		Declaration dec = symbolTable.lookup(ident);
		boolean existInScope = symbolTable.existInScope(ident);		
		check(dec != null, simpleLValue, "variable  " + ident + " does not exist in symbol table");
		check(existInScope, simpleLValue, "variable " + ident + " is undefined in current scope");	

		simpleLValue.type = dec.type;
		//<SimpleLValue>.type := IDENTIFIER.type where the type of the IDENTIFIER is obtained from the symbol table		
		return simpleLValue.type;
	}

	@Override
	public Object visitExprLValue(ExprLValue exprLValue, Object arg) throws Exception {
		//Condition: IDENTIFIER = symbol table and defined in current scope.
		String ident = exprLValue.identifier.getText();
		Declaration dec = symbolTable.lookup(ident);
		boolean existInScope = symbolTable.existInScope(ident);
		check(dec != null, exprLValue, "variable " + ident + "  does not exist in symbol table");
		check(existInScope, exprLValue, "variable " + ident + " is undefined in current scope");	
		/* Condition:
			let IDENTIFIER.type = (keyType,valType)
			in
			keyType == <Expression>.type
		 */
		Type exprType = (Type) exprLValue.expression.visit(this,arg);
		CompoundType identType = (CompoundType) dec.type;
		Type keyType = identType.keyType;
		Type valType = identType.valType;
		check(keyType.equals(exprType), exprLValue, "incompatible types in lvalue expression");

		//<ExprLValue>.type = valType
		return valType;
	}

	@Override
	public Object visitPair(Pair pair, Object arg) throws Exception {
		pair.expression0.visit(this,arg);
		pair.expression1.visit(this,arg);
		return null;
	}

	@Override
	public Object visitPairList(PairList pairList, Object arg) throws Exception {
		// All pairs in the <PairList> have the same type.
		/*Example
		{[a,b],[1,c]}
		type of a is int
		type of b is same as type of c */

		List<Pair> pList = pairList.pairs;
		if(!pList.isEmpty()){

			Type a0 = null;
			Type a1 = null;
			Type e0 = (Type) pList.get(0).expression0.visit(this, arg);
			Type e1 = (Type) pList.get(0).expression1.visit(this, arg);

			for (int i = 1; i < pList.size(); i++){
				a0 = (Type) pList.get(i).expression0.visit(this, arg);
				a1 = (Type) pList.get(i).expression1.visit(this, arg);
				check(a0.equals(e0) && (a1.equals(e1)), pairList, "incompatible types in pairList");
			}
		}

		return null;
	}

	@Override
	public Object visitLValueExpression(LValueExpression lValueExpression,
			Object arg) throws Exception {
		//<LValueExpression>.type := <LValue>.type
		Type type = (Type) lValueExpression.lValue.visit(this,arg);

		return type;
	}

	@Override
	public Object visitIntegerLiteralExpression(IntegerLiteralExpression integerLiteralExpression, Object arg) throws Exception {
		//<IntegerLiteralExpression>.type = int
		SimpleType type = new SimpleType(Kind.INT);
		return type;
	}

	@Override
	public Object visitBooleanLiteralExpression(BooleanLiteralExpression booleanLiteralExpression, Object arg) throws Exception {
		//<BooleanLiteralExpression>.type = boolean
		SimpleType type = new SimpleType(Kind.BOOLEAN);
		return type;
	}

	@Override
	public Object visitStringLiteralExpression(StringLiteralExpression stringLiteralExpression, Object arg)	throws Exception {
		//<StringLiteralExpression>.type = string
		SimpleType type = new SimpleType(Kind.STRING);
		return type;
	}

	@Override
	public Object visitUnaryOpExpression(UnaryOpExpression unaryOpExpression, Object arg) throws Exception {
		//Condition: op == - or op == !
		Kind op = unaryOpExpression.op;		
		check(op.equals(Kind.MINUS) || op.equals(Kind.NOT), unaryOpExpression, "not a unary op expression");

		//if op = - then <Expression>.type = int && if op = ! then <Expression>.type = boolean
		SimpleType type = null;
		if(op == Kind.MINUS)
			type = new SimpleType(Kind.INT);
		else
			type = new SimpleType(Kind.BOOLEAN);

		return type;
	}

	@Override
	public Object visitBinaryOpExpression(BinaryOpExpression binaryOpExpression, Object arg) throws Exception {
		boolean typesNotEqual = false;
		Kind op = binaryOpExpression.op;
		Type e0Type = (Type) binaryOpExpression.expression0.visit(this, arg);
		Type e1Type = (Type) binaryOpExpression.expression1.visit(this, arg);
		Type exprType = null;

		//<Expression0>.type == <Expression1>.type unless the op is a + and one of them is a string and the other an int or boolean.
		if(op.equals(Kind.PLUS)){
			if( (e0Type.type.equals(Kind.STRING) && (e1Type.type.equals(Kind.INT) ||e1Type.type.equals(Kind.BOOLEAN))) ||
					(e1Type.type.equals(Kind.STRING) && (e0Type.type.equals(Kind.INT) ||e0Type.type.equals(Kind.BOOLEAN)))){
				typesNotEqual = true;
			}		 
		}
		if(typesNotEqual == false)
			check(e0Type.equals(e1Type), binaryOpExpression, "incompatibe types in binaryOpExpressions");

		// + can be applied to all types except boolean. The type is the type of the result. 
		//If one of the arguments is a string, then the result is a string.
		if(op.equals(Kind.PLUS)) {
			if(!e0Type.type.equals(Kind.BOOLEAN)) {
				if(e0Type.type.equals(Kind.STRING) || e1Type.type.equals(Kind.STRING))
					exprType = new SimpleType(Kind.STRING);
				else
					exprType = e0Type;
			}
			else
				check(false, binaryOpExpression, "cannot add type boolean");
		}

		//==, !=, >, <, ≤, ≥ apply to any type and the result is boolean
		if(op.equals(Kind.EQUALS) || op.equals(Kind.NOT_EQUALS) ||op.equals(Kind.GREATER_THAN) 
				||op.equals(Kind.LESS_THAN) ||op.equals(Kind.AT_MOST) ||op.equals(Kind.AT_LEAST))
			exprType = new SimpleType(Kind.BOOLEAN);

		// * and - can be applied to integers and maps. The result is the same as the argument type
		if(op.equals(Kind.TIMES) || op.equals(Kind.MINUS)){
			if(e0Type.type.equals(Kind.INT))
				exprType = e0Type;
			else if(e0Type.type.equals(Kind.MAP))
				exprType = e0Type;
			else
				check(false, binaryOpExpression, "expecting type int or map in binaryOpExpressions");
				
		}

		// / can be applied to integers, the result is the same as the argument type.
		if(op.equals(Kind.DIVIDE)){
			if(e0Type.type.equals(Kind.INT))
				exprType = e0Type;
			else
				check(false, binaryOpExpression, "expecting type int in binaryOpExpressions");
		}

		// & and | can be applied to boolean types, the result is a boolean.
		if(op.equals(Kind.AND) || op.equals(Kind.OR)){
			if(e0Type.type.equals(Kind.BOOLEAN))
				exprType = e0Type;
			check(false, binaryOpExpression, "expecting type boolean in binaryOpExpressions");
		}

		return exprType;
	}

}
