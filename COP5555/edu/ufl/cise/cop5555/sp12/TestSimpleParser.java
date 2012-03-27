package edu.ufl.cise.cop5555.sp12;

import static edu.ufl.cise.cop5555.sp12.Kind.*;
import static org.junit.Assert.*;

import org.junit.Test;

import edu.ufl.cise.cop5555.sp12.Scanner;
import edu.ufl.cise.cop5555.sp12.SimpleParser;
import edu.ufl.cise.cop5555.sp12.TokenStream;

public class TestSimpleParser {
   
	private TokenStream getInitializedTokenStream(String input) {
		TokenStream stream = new TokenStream(input);
		Scanner s = new Scanner(stream);
		s.scan();
		return stream;
	}
	
	
	@Test
	public void testEmptyProg(){
		String input = "prog Test1 gorp";
		TokenStream stream = getInitializedTokenStream(input);
		SimpleParser parser = new SimpleParser(stream);
		SyntaxException result = parser.parse();
		assertNull(result);
	}
	
	@Test
	public void testIntDec(){
		String input = "prog Test1 int x; gorp";
		TokenStream stream = getInitializedTokenStream(input);
		SimpleParser parser = new SimpleParser(stream);
		SyntaxException result = parser.parse();
		assertNull(result);
	}
	
	@Test
	public void testBooleanDec(){
		String input = "prog Test1 boolean x; gorp";
		TokenStream stream = getInitializedTokenStream(input);
		SimpleParser parser = new SimpleParser(stream);
		SyntaxException result = parser.parse();
		assertNull(result);
	}
	
	@Test
	public void testMapDec(){
		String input = "prog Test1 map[int,string] y; gorp";
		TokenStream stream = getInitializedTokenStream(input);
		SimpleParser parser = new SimpleParser(stream);
		SyntaxException result = parser.parse();
		assertNull(result);
	}
	
	@Test
	public void testMapDec2(){
		String input = "prog Test1 map[int,map[string,boolean]] m; gorp";
		TokenStream stream = getInitializedTokenStream(input);
		SimpleParser parser = new SimpleParser(stream);
		SyntaxException result = parser.parse();
		assertNull(result);
	}
	
	@Test
	public void xtestMapDec2(){
		String input = "prog Test1 map[int,map[string,boolean]] ; gorp";  //semi is the error, should be ident
		TokenStream stream = getInitializedTokenStream(input);
		SimpleParser parser = new SimpleParser(stream);
		SyntaxException result = parser.parse();
		assertNotNull(result);
		assertEquals(SEMI, result.t.kind);
	}
}
