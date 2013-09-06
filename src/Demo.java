import java.io.File;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.TextProgressMonitor;

public class Demo {
	
	public static void main(String args[]) throws Exception
	{
		File tmpDir = new File(System.getProperty("java.io.tmpdir"), "tmp"
				+ System.currentTimeMillis());
		tmpDir.mkdirs();
		
		try {
			Git r = Git.cloneRepository().setBare(true).setBranch("master").setDirectory(tmpDir).setURI(
					"git@github.com:czw200809/git_repo.git")
					.setProgressMonitor(new TextProgressMonitor()).call();
			/*r.checkout().setName("origin/master").call();
			for (Ref f : r.branchList().setListMode(ListMode.ALL).call()) {
				r.checkout().setName(f.getName()).call();
				System.out.println("checked out branch " + f.getName()
						+ ". HEAD: " + r.getRepository().getRef("HEAD"));
			}
			// try to checkout branches by specifying abbreviated names
			r.checkout().setName("master").call();
			r.checkout().setName("origin/master").call();
			try {
				r.checkout().setName("master").call();
			} catch (RefNotFoundException e) {
				System.err.println("couldn't checkout 'test'. Got exception: "
						+ e.toString() + ". HEAD: "
						+ r.getRepository().getRef("HEAD"));
			}*/
			r.getRepository().close();
		} finally {
			rm(tmpDir);
		}
	}

	static void rm(File f) {
		if (f.isDirectory())
			for (File c : f.listFiles())
				rm(c);
		f.delete();
	}

}
