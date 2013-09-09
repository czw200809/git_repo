import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
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
		String filePath = "src";
		
		Repository repository = null;
		List<String> pathList = new ArrayList<String>();
		List<String> nameList = new ArrayList<String>();
		try {
			
			Git git = Git.open(new File(url));
			
			repository = git.getRepository();
			//ObjectId objId = repository.resolve(Constants.HEAD);
			ObjectId objId = repository.resolve("afb3add42ddead3e40847ecad06d533a96076c58");
			//Ref head = repository.getRef("HEAD");

			RevWalk walk = new RevWalk(repository);

			//RevCommit commit = walk.parseCommit(head.getObjectId());
			RevCommit commit = walk.parseCommit(objId);
			RevTree tree = commit.getTree();
			//System.out.println("Having tree: " + tree);
			
			TreeWalk treeWalk = new TreeWalk(repository);
			treeWalk.setFilter(PathFilter.create(filePath));
			treeWalk.addTree(tree);
			//treeWalk.setRecursive(true);
			
			int count = 0;
			String path = null;
			while(treeWalk.next()) {
				
				path = treeWalk.getPathString();
				if (count > 0 && (path.indexOf(filePath) != -1))
				{
					if (!path.equals(filePath))
					{
						nameList.add(treeWalk.getNameString());
						pathList.add(treeWalk.getPathString());
					}
				}
			    System.out.println("Folder Path: " + treeWalk.getPathString());
			    System.out.println("Folder Name: " + treeWalk.getNameString());
			    System.out.println("-----------------------------------------");
			    if (count <= 0 || path.indexOf(filePath) == -1 || path.equals(filePath))
			    	treeWalk.enterSubtree();
			    count++;
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
