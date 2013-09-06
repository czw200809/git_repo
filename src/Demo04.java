import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;


public class Demo04 {

	public static void main(String[] args) throws Exception 
	{
		String url = "D:/MyEclipse_Space/git_project/.git";

		File gitDir = new File(url);
		Repository repository = new FileRepository(gitDir);

		Ref head = repository.getRef("HEAD");

		RevWalk walk = new RevWalk(repository);

		RevCommit commit = walk.parseCommit(head.getObjectId());
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
	}
}
