import java.io.ByteArrayOutputStream;
import java.io.File;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

public class GitDemo {

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println("Usage: GitTest /path/to/repo");
			System.exit(1);
		}

		String filename = args[0];

		checkRepo(filename);

		Repository repository = getRepository(filename);
		String branch = "refs/heads/master";

		ObjectId branchObjectId = repository.resolve(branch);
		if (branchObjectId == null) {
			throw new Exception("branch " + branch + " could not be resolved");
		}
		RevWalk revWalk = new RevWalk(repository);
		RevCommit commit = revWalk.parseCommit(branchObjectId);
		System.err.println("commit: " + commit.getName());
		RevTree tree = commit.getTree();
		TreeWalk treeWalk = new TreeWalk(repository);
		treeWalk.addTree(tree);
		treeWalk.setRecursive(true);
		treeWalk.setFilter(TreeFilter.ALL);
		while (treeWalk.next()) {
			String path = new String(treeWalk.getRawPath(), "UTF-8");
			System.out
					.println(path + ", " + treeWalk.getDepth() + ", "
							+ treeWalk.getNameString() + ", "
							+ treeWalk.getTreeCount());
		}

	}

	public static void checkRepo(String path) throws Exception {
		Repository repo = getRepository(path);
		System.err.println("dir: " + repo.getConfig());
		System.err.println("bare: " + repo.isBare());
		System.err.println("master: " + repo.resolve("refs/heads/master"));
		repo.close();
	}

	public static Repository getRepository(String path) throws Exception {
		File file = new File(path);
		if (!file.isDirectory()) {
			throw new Exception("Path " + path + " does not exist");
		}

		File gitFile = new File(file, ".git");
		if (gitFile.isDirectory()) {
			file = gitFile;
		}

		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		builder.setGitDir(file);
		builder.readEnvironment();
		builder.findGitDir();
		if (builder.getGitDir() == null) {
			throw new Exception("Path " + path + " is not a git repository");
		}
		return builder.build();
	}

	public static byte[] getData(Repository repository, String branch,
			String filename) throws Exception {
		ObjectId branchObjectId = repository.resolve(branch);
		if (branchObjectId == null) {
			throw new Exception("branch " + branch + " could not be resolved");
		}
		RevWalk revWalk = new RevWalk(repository);
		RevCommit commit = revWalk.parseCommit(branchObjectId);
		RevTree tree = commit.getTree();
		TreeWalk treeWalk = new TreeWalk(repository);
		treeWalk.addTree(tree);
		treeWalk.setRecursive(true);
		treeWalk.setFilter(PathFilter.create(filename));
		if (!treeWalk.next()) {
			throw new Exception("Not found");
		}
		ObjectId objectId = treeWalk.getObjectId(0);
		ObjectLoader loader = repository.open(objectId);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		loader.copyTo(bos);
		return bos.toByteArray();
	}

}