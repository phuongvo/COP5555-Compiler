package cop5555_sources;

import java.util.HashMap;

public class Demo {
	static String s,t;
	static int x,y;
	static boolean b,c;
//	static HashMap m;
	
	public static void main(String[] args){
		b = false;
		c = false;
		x = 5;
		y = 6;
		s = "hi";
		t = "hi";
		
		System.out.println();
		
		boolean res = s.compareTo(t) == 0;
		
		if(res == true)
			System.out.print(s);
		if(b == false){
			System.out.println(s);}
		if(s.compareTo("hi") == 0){
			System.out.println(s);}
		
		
	/*	if(b==false)
			s = "d";	
		else
			s = "g";*/
		


	}
	
	/*public static void main(String[] args){
		b = true;
		s = "Go Gators";
		m = new HashMap();
		System.out.print(x);
		System.out.print(true);
		System.out.print(s);

		System.out.print("g");
		
		if(b==true)
			b = false;
		
		do{
			 b = false;
		}while(b == true);
		
		
	}*/
}