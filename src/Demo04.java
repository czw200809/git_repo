import java.io.File;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;


public class Demo04 {

	public static void main(String[] args) throws Exception 
	{
		//String url = "D:/MyEclipse_Space/git_project/.git";
		//https://github.com/czw200809/git_repo.git
		String url = "https://github.com/czw200809/git_repo.git";
		File tmpDir = new File(System.getProperty("java.io.tmpdir"), "tmp"
				+ System.currentTimeMillis());
		
		tmpDir.mkdirs();
		Repository repository = null;
		try {
			
			Git git = Git.cloneRepository().setBare(true).setBranch("master").setDirectory(tmpDir).setURI(
					"git@github.com:czw200809/git_repo.git")
					.setProgressMonitor(new TextProgressMonitor()).call();
			
			repository = git.getRepository();
			//Ref head = repository.getRef("HEAD");

			RevWalk walk = new RevWalk(repository);
			
			ObjectId objId = repository.resolve("9b78b310ed0d2be654b8d08710e589c3bbbd0633");
			RevCommit commit = walk.parseCommit(objId);
			//RevCommit commit = walk.parseCommit(head.getObjectId());
			RevTree tree = commit.getTree();
			System.out.println("Having tree: " + tree);
			
			//TreeWalk treeWalk = TreeWalk.forPath(repository, "src", tree);
			TreeWalk treeWalk = new TreeWalk(repository);
			//treeWalk.setFilter(PathFilter.create("src"));
			treeWalk.addTree(tree);
			//treeWalk.setRecursive(true);
			
			while(treeWalk.next()) {
			    System.out.println("Folder Path: " + treeWalk.getPathString());
			    System.out.println("Folder Name: " + treeWalk.getNameString());
			    //System.out.println("Folder depth: " + treeWalk.getDepth());
			    //System.out.println("Folder Tree Count: " + treeWalk.getTreeCount());
			    System.out.println("-----------------------------------------");
			    treeWalk.enterSubtree();
			}
			
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			if (repository != null)
			{
				repository.close();
				rm(tmpDir);
			}
		}
	}
	
	static void rm(File f) {
		if (f.isDirectory())
			for (File c : f.listFiles())
				rm(c);
		f.delete();
	}
}
