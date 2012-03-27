package edu.ufl.cise.cop5555.sp12.context;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Stack;

import edu.ufl.cise.cop5555.sp12.ast.Declaration;

public class SymbolTable {

	private Hashtable<String, ArrayList<Declaration>> symbolTable;
	private Hashtable<String, ArrayList<Integer>> scopeTable;
	private Stack<Integer> scope_stack;
	private int current_scope, next_scope;


	public SymbolTable(){
		symbolTable = new Hashtable<String, ArrayList<Declaration>>();
		scopeTable = new Hashtable<String, ArrayList<Integer>>();
		scope_stack = new Stack<Integer>();
		current_scope = 0;
		next_scope = 0;
	}

	public void enterScope() {
		current_scope = next_scope++;
		scope_stack.push(current_scope);
	}

	public void exitScope() {
		scope_stack.pop();	
		if(!scope_stack.empty())
			current_scope = scope_stack.get(scope_stack.size()-1);
	}

	// returns the in-scope declaration of the name if there is one, 
	//otherwise it returns null	
	public Declaration lookup(String ident) {		
		//return null if no declaration exists 
		if(symbolTable.isEmpty())
			return null;

		ArrayList<Declaration> decList = symbolTable.get(ident);
		ArrayList<Integer> scopeList = scopeTable.get(ident);

		for(int i = scope_stack.size()-1; i >= 0; i--){
			for(int j = scopeList.size() - 1; j >= 0; j--){
				if(scopeList.get(j) == scope_stack.get(i)){
					return decList.get(j);
				}
			}
		}	
		return null;

	}

	// if the name is already declared IN THE CURRENT SCOPE, returns false. 
	//Otherwise inserts the declaration in the symbol table
	public boolean insert(String ident, Declaration dec) {

		//check if variable is declared in current_scope		
		if(existInCurrentScope(ident))
			return false;

		//Insert declaration and scope into respective tables

		ArrayList<Declaration> decList = new ArrayList<Declaration>();
		ArrayList<Integer> scopeList = new ArrayList<Integer>();

		if(!(symbolTable.get(ident) == null)){
			decList = symbolTable.get(ident);	
			scopeList = scopeTable.get(ident);
		}

		decList.add(dec);
		scopeList.add(current_scope);

		symbolTable.put(ident, decList);	//add dec to symbol table
		scopeTable.put(ident, scopeList);	//add current scope to scope table

		return true;
	}

	public boolean existInCurrentScope(String ident) {	

		if(scopeTable.get(ident) == null)
			return false;
		
		ArrayList<Integer> identScopeList = scopeTable.get(ident);
			for(int j = identScopeList.size() -1; j >= 0; j--) {
				if(identScopeList.get(j) == current_scope)
					return true;	//return false if exists in currentscope list
			}					
		return false;
	}	
	
	public boolean existInScope(String ident) {	

		if(scopeTable.get(ident) == null)
			return false;
		
		ArrayList<Integer> identScopeList = scopeTable.get(ident);
		for(int i = scope_stack.size() -1; i >= 0; i--){
			for(int j = identScopeList.size() -1; j >= 0; j--) {
				if(identScopeList.get(j) == scope_stack.get(i)) 
					return true;	//return false if exists in scope list
			}					
		}
		return false;
	}	
}