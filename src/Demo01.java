import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.revwalk.RevCommit;

import com.wbf.git.dto.GitLogDto;

public class Demo01 {
	
	public static void main(String args[]) throws Exception
	{
		String dir = "tmp" + System.currentTimeMillis();
		String str = System.getProperty("java.io.tmpdir");
		File tmpDir = new File(System.getProperty("java.io.tmpdir"), dir);
		
		tmpDir.mkdirs();
		
		try {
			Git git = Git.cloneRepository().setBare(true).setBranch("master").setDirectory(tmpDir).setURI(
					"D:/MyEclipse_Space/git_project/.git")
					.setProgressMonitor(new TextProgressMonitor()).call();
			
			List<GitLogDto> logDtoList = null;
	        
	        Repository repo = git.getRepository();
	        ObjectId objId = repo.resolve("master");
	        
	        Iterable<RevCommit> revCommits = revCommits = git.log().add(objId).call();
	       
	        if (revCommits != null)
	        {
	        	logDtoList = new ArrayList<GitLogDto>();
	        	RevCommit revCommit = null;
	        	GitLogDto logDto = null;
	        	for (Iterator<RevCommit> iter = revCommits.iterator(); iter.hasNext();)
	        	{
	        		revCommit = iter.next();
	        		System.out.println(revCommit.getFullMessage());
	        	}
	        }

		} finally {
			del(tmpDir);
		}
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
