import java.io.File;


public class Demo02 {

	public static void main(String[] args) {
		
		//File file = new File("d:/a");
		File file = new File("C:\\Users\\username\\AppData\\Local\\Temp\\tmp1378200875503");
		
		del(file);
	}
	
	
	public static void del(File file)
	{
		if (file.isDirectory())
		{
			File[] files = file.listFiles();
			for (int i = 0; i < files.length; i++)
			{
				del(files[i]);
			}
		}
		
		file.delete();
	}
}
