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


public class Demo05 {

	public static void main(String[] args) throws Exception 
	{
		String url = "D:/MyEclipse_Space/git_project/.git";
		
		Repository repository = null;
		try {
			
			Git git = Git.open(new File(url));
			
			repository = git.getRepository();
			ObjectId objId = repository.resolve("8a81d0841b70cc11054cd53bde5d07f36bc46c35");
			//Ref head = repository.getRef("HEAD");

			RevWalk walk = new RevWalk(repository);

			//RevCommit commit = walk.parseCommit(head.getObjectId());
			RevCommit commit = walk.parseCommit(objId);
			RevTree tree = commit.getTree();
			System.out.println("Having tree: " + tree);
			
			//TreeWalk treeWalk = TreeWalk.forPath(repository, "src", tree);
			TreeWalk treeWalk = new TreeWalk(repository);
			treeWalk.setFilter(PathFilter.create("src"));
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
				//rm(tmpDir);
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
