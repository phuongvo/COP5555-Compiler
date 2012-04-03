package edu.ufl.cise.cop5555.sp12.codegen;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import edu.ufl.cise.cop5555.sp12.Kind;
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

public class CodeGenVisitor implements ASTVisitor, Opcodes {

	String className;
	ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
	FieldVisitor fv;
	MethodVisitor mv;
	AnnotationVisitor av0;

	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		className = program.ident.getText();
		cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object", null);
		mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
		mv.visitCode();
		//set label on first instruction of main method
		Label lstart = new Label();
		mv.visitLabel(lstart);
		//visit block to generate code and field declarations
		program.block.visit(this,null);
		//add return instruction
		mv.visitInsn(RETURN);
		Label lend= new Label();
		mv.visitLabel(lend);
		//visit local variable--the only one in our project is the String[] argument to the main method
		mv.visitLocalVariable("args", "[Ljava/lang/String;", null, lstart, lend, 0);
		mv.visitMaxs(2, 1);
		mv.visitEnd();
		cw.visitEnd();
		//convert class file to byte array and return
		return cw.toByteArray();
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws Exception {
		//visit children
		for (DecOrCommand cd : block.decOrCommands) {
			cd.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitDeclaration(Declaration declaration, Object arg)
			throws Exception {
		/* Add a static field with name given by the indicator 
			and appropriate type to the classfile */

		String fieldName = declaration.ident.getText();  
		String fieldType = (String) declaration.type.visit(this, null);		
		fv = cw.visitField(ACC_STATIC, fieldName, fieldType, null, null);
		fv.visitEnd();

		return null;
	}

	@Override
	public Object visitSimpleType(SimpleType simpleType, Object arg)
			throws Exception {		
		//These types are represented by java int, boolean, and java.lang.String respectively.
		Kind kind = simpleType.type;
		switch (kind) {							
		case INT:
			return "I";
		case STRING:
			return "Ljava/lang/String;";
		case BOOLEAN:
			return "Z";
		}

		return null;
	}

	@Override
	public Object visitCompoundType(CompoundType compoundType, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitAssignExprCommand(AssignExprCommand assignExprCommand,
			Object arg) throws Exception {
		// Generate code to evaluate the expression and store the 
		//results in the variable indicated by the LValue
		String varName = (String) assignExprCommand.lValue.visit(this, null);	

		String type = (String) assignExprCommand.expression.visit(this, null);		
		mv.visitFieldInsn(PUTSTATIC, className, varName, type);

		return null;
	}

	@Override
	public Object visitAssignPairListCommand(
			AssignPairListCommand assignPairListCommand, Object arg)
					throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitPrintCommand(PrintCommand printCommand, Object arg)
			throws Exception {
		//Generate code to invoke System.out.print with the value of the Expression as a parameter.
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		String fieldType = (String) printCommand.expression.visit(this, null);
		String type = "("+fieldType+")V";		
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "print", type);			

		return null;
	}

	@Override
	public Object visitPrintlnCommand(PrintlnCommand printlnCommand, Object arg)
			throws Exception {
		// Generate code to invoke System.out.println with the value of the Expression as a parameter.
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		String fieldType = (String) printlnCommand.expression.visit(this, null);
		String type = "("+fieldType+")V";
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", type);
		return null;
	}

	@Override
	public Object visitDoCommand(DoCommand doCommand, Object arg)
			throws Exception {
		// Generate as if for a Java while loop.

		Label test = new Label();
		Label block = new Label();
		Label end = new Label();

		mv.visitLabel(test);
		doCommand.expression.visit(this, null);
		mv.visitJumpInsn(IFEQ, end); //if false
		mv.visitLabel(block);
		doCommand.block.visit(this, null);	
		mv.visitJumpInsn(GOTO, test);
		mv.visitLabel(end);


		return null;
	}

	@Override
	public Object visitDoEachCommand(DoEachCommand doEachCommand, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitIfCommand(IfCommand ifCommand, Object arg)
			throws Exception {
		// Generate code as if for a Java if statement
		ifCommand.expression.visit(this,arg); 
		Label skip = new Label();
		mv.visitJumpInsn(IFEQ, skip);	//if false
		ifCommand.block.visit(this,arg); 		
		mv.visitLabel(skip);	
		return null;
	}

	@Override
	public Object visitIfElseCommand(IfElseCommand ifElseCommand, Object arg)
			throws Exception {
		// Generate code as if for a Java if-else statement

		Label elsestmt = new Label();
		Label end = new Label();

		ifElseCommand.expression.visit(this,arg); 
		mv.visitJumpInsn(IFEQ, elsestmt);	//if false
		ifElseCommand.ifBlock.visit(this,arg); 	 
		mv.visitJumpInsn(GOTO, end);
		mv.visitLabel(elsestmt);			
		ifElseCommand.elseBlock.visit(this,arg); 
		mv.visitLabel(end);
		return null;
	}

	@Override
	public Object visitSimpleLValue(SimpleLValue simpleLValue, Object arg)
			throws Exception {
		// When this appears on the lhs of an assignment, use as target of the assignment
		return simpleLValue.identifier.getText();
	}

	@Override
	public Object visitExprLValue(ExprLValue exprLValue, Object arg)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitPair(Pair pair, Object arg) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitPairList(PairList pairList, Object arg) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitLValueExpression(LValueExpression lValueExpression,
			Object arg) throws Exception {
		// Generate code to leave the value of the expression on top of the stack
		String varName = (String) lValueExpression.lValue.visit(this, null);	
		String type = (String) lValueExpression.lValue.type.javaType;

		mv.visitFieldInsn(GETSTATIC, className, varName, type);		
		return type;
	}

	@Override
	public Object visitIntegerLiteralExpression(
			IntegerLiteralExpression integerLiteralExpression, Object arg)
					throws Exception {
		//gen code to leave value of literal on top of stack

		mv.visitLdcInsn(integerLiteralExpression.integerLiteral.getIntVal());
		return "I";
	}

	@Override
	public Object visitBooleanLiteralExpression(
			BooleanLiteralExpression booleanLiteralExpression, Object arg)
					throws Exception {
		//gen code to leave value of boolean on top of stack
		if(booleanLiteralExpression.booleanLiteral.getBoolVal() == 1)
			mv.visitInsn(ICONST_1);
		else
			mv.visitInsn(ICONST_0);	
		return "Z";
	}

	@Override
	public Object visitStringLiteralExpression(
			StringLiteralExpression stringLiteralExpression, Object arg)
					throws Exception {
		//gen code to leave value of string on top of stack
		String s = stringLiteralExpression.stringLiteral.getText();
		mv.visitLdcInsn(s);		
		return "Ljava/lang/String;";
	}

	@Override
	public Object visitUnaryOpExpression(UnaryOpExpression unaryOpExpression,
			Object arg) throws Exception {
		// if op is -, negate the value on top of the stack, 
		//if op is !, logically negate the value on top of the stack.
		Kind op = unaryOpExpression.op;	
		String type = (String) unaryOpExpression.expression.visit(this, null);
	
		switch(op){
		case MINUS:	//int
			mv.visitInsn(INEG);
			break;
		case NOT:	//boolean			
			Label not = new Label();
			mv.visitJumpInsn(IFEQ, not);
			mv.visitInsn(ICONST_0);
			Label not2 = new Label();
			mv.visitJumpInsn(GOTO, not2);
			mv.visitLabel(not);
			mv.visitInsn(ICONST_1);
			mv.visitLabel(not2);
			break;
		}
		return type;
	}

	@Override
	public Object visitBinaryOpExpression(
			BinaryOpExpression binaryOpExpression, Object arg) throws Exception {
		// Generate code to leave the value of the expression on top of the stack.

		Kind op = binaryOpExpression.op;
		String type1 = (String) binaryOpExpression.expression0.visit(this, null); //put first value on stack
		String type2 = (String) binaryOpExpression.expression1.visit(this, null); //put second value on stack

		switch(op){
		case EQUALS:		
			if(type1.compareTo("I") == 0 || type1.compareTo("Z") == 0){

				Label neq = new Label();
				mv.visitJumpInsn(IF_ICMPNE, neq);

				mv.visitInsn(ICONST_1);
				Label end = new Label();
				mv.visitJumpInsn(GOTO, end);

				mv.visitLabel(neq);
				mv.visitInsn(ICONST_0);

				mv.visitLabel(end);
			}
			else if(type1.compareTo("Ljava/lang/String;") == 0){				
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "compareTo", "(Ljava/lang/String;)I");
				Label ne = new Label();
				mv.visitJumpInsn(IFNE, ne);

				mv.visitInsn(ICONST_1);				
				Label end = new Label();
				mv.visitJumpInsn(GOTO, end);

				mv.visitLabel(ne);
				mv.visitInsn(ICONST_0);

				mv.visitLabel(end);	
			}
			type1 = "Z";
			break;
		case NOT_EQUALS:		
			if(type1.compareTo("I") == 0 || type1.compareTo("Z") == 0){

				Label neq = new Label();
				mv.visitJumpInsn(IF_ICMPEQ, neq);

				mv.visitInsn(ICONST_1);
				Label end = new Label();
				mv.visitJumpInsn(GOTO, end);

				mv.visitLabel(neq);
				mv.visitInsn(ICONST_0);

				mv.visitLabel(end);
			}
			else if(type1.compareTo("Ljava/lang/String;") == 0){				
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "compareTo", "(Ljava/lang/String;)I");
				Label ne = new Label();
				mv.visitJumpInsn(IFEQ, ne);

				mv.visitInsn(ICONST_1);				
				Label end = new Label();
				mv.visitJumpInsn(GOTO, end);

				mv.visitLabel(ne);
				mv.visitInsn(ICONST_0);

				mv.visitLabel(end);	
			}
			type1 = "Z";
			break;
		case PLUS:
			//addition
			if(type1.compareTo("I") == 0 && type2.compareTo("I") == 0){
				mv.visitInsn(IADD);
			}
			//string concatenation
			else if(type1.compareTo("Ljava/lang/String;") == 0 || type2.compareTo("Ljava/lang/String;") == 0){
				mv.visitInsn(SWAP);
				mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
				mv.visitInsn(DUP);				
				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V");
				mv.visitInsn(SWAP);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "("+type1+")Ljava/lang/StringBuilder;");
				mv.visitInsn(SWAP);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "("+type2+")Ljava/lang/StringBuilder;");
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");	
				type1 = "Ljava/lang/String;";
			}
			break;
		case MINUS:
			if(type1.compareTo("I") == 0){	//integer
				mv.visitInsn(ISUB);
			}
			else {} //map to be implemented
			break;
		case TIMES:
			mv.visitInsn(IMUL);
			break;
		case DIVIDE:
			mv.visitInsn(IDIV);
			break;
		case AND:
			Label l1 = new Label();
			mv.visitJumpInsn(IFEQ, l1);
			mv.visitInsn(ICONST_0);
			Label endl1 = new Label();
			mv.visitJumpInsn(GOTO, endl1);
			mv.visitLabel(l1);
			mv.visitInsn(ICONST_1);
			mv.visitLabel(endl1);
			break;
		case OR:
			Label l2 = new Label();
			mv.visitJumpInsn(IFEQ, l2);
			mv.visitInsn(ICONST_1);
			Label endl2 = new Label();
			mv.visitJumpInsn(GOTO, endl2);
			mv.visitLabel(l2);
			mv.visitInsn(ICONST_0);
			mv.visitLabel(endl2);
			type1 = "Z";
			break;
		case LESS_THAN:	// <
			Label lt = new Label();
			mv.visitJumpInsn(IF_ICMPGE, lt);
			mv.visitInsn(ICONST_1);
			Label lt2 = new Label();
			mv.visitJumpInsn(GOTO, lt2);
			mv.visitLabel(lt);
			mv.visitInsn(ICONST_0);
			mv.visitLabel(lt2);
			type1 = "Z";
			break;
		case GREATER_THAN:	// >
			Label gt = new Label();
			mv.visitJumpInsn(IF_ICMPLE, gt);
			mv.visitInsn(ICONST_1);
			Label gt2 = new Label();
			mv.visitJumpInsn(GOTO, gt2);
			mv.visitLabel(gt);
			mv.visitInsn(ICONST_0);
			mv.visitLabel(gt2);
			type1 = "Z";
			break;
		case AT_MOST:	// <= 
			if(type1.compareTo("I") == 0){
				Label at = new Label();
				mv.visitJumpInsn(IF_ICMPGT, at);
				mv.visitInsn(ICONST_1);
				Label at2 = new Label();
				mv.visitJumpInsn(GOTO, at2);
				mv.visitLabel(at);
				mv.visitInsn(ICONST_0);
				mv.visitLabel(at2);
			}
			else if (type1.compareTo("Ljava/lang/String;") == 0){ //Prefix or equals relation (invoke the startsWith method of the String class)
				mv.visitInsn(SWAP);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z");				
			}
			type1 = "Z";
			break;
		case AT_LEAST:
			Label al = new Label(); // >=
			mv.visitJumpInsn(IF_ICMPLT, al);
			mv.visitInsn(ICONST_1);
			Label al2 = new Label();
			mv.visitJumpInsn(GOTO, al2);
			mv.visitLabel(al);
			mv.visitInsn(ICONST_0);
			mv.visitLabel(al2);
			type1 = "Z";
			break;
		}

		return type1;
	}



}
