package edu.ufl.cise.cop5555.sp12.ast;

import edu.ufl.cise.cop5555.sp12.Kind;

public class CompoundType extends Type {
	
	public final SimpleType keyType;
	public final Type valType;

	public CompoundType(SimpleType keyType, Type valType) {
		this.keyType = keyType;
		this.valType = valType;
		this.type = Kind.MAP;
	}

	@Override
	public Object visit(ASTVisitor v, Object arg) throws Exception {
		return v.visitCompoundType(this, arg);
	}

	@Override
	public boolean equals(Object obj) {
	    if (obj == null) return false;
	    if (obj == this) return true;
	    
		CompoundType type = (CompoundType) obj;
		if(this.keyType.type == type.keyType.type)
			return this.valType.type == type.valType.type;
		else
			return false;
	}
}
