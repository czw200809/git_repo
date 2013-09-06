import java.util.Date;


public class Demo03 {

	public static void main(String[] args) {
		
		Date d1 = new Date();
		long t1 = d1.getTime();
		for (int i = 0; i < 100; i++)
		{
			System.out.print("a" + i);
		}
		
		Date d2 = new Date();
		long t2 = d2.getTime();
		
		int a = 10;
	}

}
