import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;


public class RepoUtils {
	
	public static Repository openRepository(String gitRoot) {
		
		File file = new File(gitRoot);
		Git git = null;
		Repository repository = null;
		
		try {
			git = Git.open(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (git != null)
		{			
			repository = git.getRepository();
		}
		
		return repository;
	}
	
	public static Git getGit(Repository repo) {
		
		Git git = new Git(repo);
		return git;
	}
	
	
}
