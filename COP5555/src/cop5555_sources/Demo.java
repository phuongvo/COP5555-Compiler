package cop5555_sources;

public class Demo {
	static String s,t, s1;
	static int x,y,z;
	static boolean b,c,d;
//	static HashMap m;
	
	public static void main(String[] args){
		b = true;
		c = false;
		x = 50;
		y = 6;
		s = "hi";
		t = "hit";
		
		System.out.print("startswith");
		
		b = "a".startsWith("an");

		System.out.println(b);
		
		System.out.print(-x);
		d = !b;
		
		while(b) {
			System.out.print(s);
			System.out.print(t);
			b = false;
		}
		
	/*	boolean res = s.compareTo(t) == 0;
		
		if(res == true)
			System.out.print(s);
		else
			System.out.print(y);*/
		
	//	string = s + t;

	}

}