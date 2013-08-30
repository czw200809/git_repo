package com.wbf.git.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.jgit.util.FS;

import com.wbf.git.dto.GitDiffStatusDto;
import com.wbf.git.dto.GitDirEntryDto;
import com.wbf.git.dto.GitLogDto;

public class GitService 
{	
	private final static String GIT = ".git";
	private final static String HEAD = "HEAD";
    private final static String REF_REMOTES = "refs/remotes/origin/";  
	
  //获取某个文件或所有文件，从版本startRevision至untilRevision的log
    //如果untilRevision == null, 则untilRevision = "HEAD"
	public static List<GitLogDto> getLog(String gitRoot, String startRevision, String untilRevision, String filePath) throws Exception 
	{
		File rootDir = new File(gitRoot);  
		
        if (new File(gitRoot + File.separator + GIT).exists() == false) {  
            Git.init().setDirectory(rootDir).call();  
        }  
        
        List<GitLogDto> logDtoList = null;
        Git git = Git.open(rootDir);
        Repository repo = git.getRepository();
        ObjectId startObjId = repo.resolve(startRevision);
        ObjectId untilObjId = null;
        if (untilRevision != null)
        	untilObjId = repo.resolve(untilRevision);
        else
        	untilObjId = repo.resolve(HEAD);
        Iterable<RevCommit> revCommits = null;
        if (filePath == null)
        	revCommits = git.log().addRange(startObjId, untilObjId).call();
        else
        	revCommits = git.log().addPath(filePath).addRange(startObjId, untilObjId).call();
        
        if (revCommits != null)
        {
        	logDtoList = new ArrayList<GitLogDto>();
        	RevCommit revCommit = null;
        	GitLogDto logDto = null;
        	for (Iterator<RevCommit> iter = revCommits.iterator(); iter.hasNext();)
        	{
        		revCommit = iter.next();
        		logDto = new GitLogDto(revCommit);
        		logDtoList.add(logDto);
        	}
        	
        	//获取startRevision这个版本的log,因为前面的范围中是不包括startRevision这个版本
        	logDto = getSpecificLog(gitRoot, startRevision, filePath);
        	logDtoList.add(logDto);
        }
        
		return logDtoList;
	}
	
	//获取指定版本的log信息/指定File且指定版本的log
	public static GitLogDto getSpecificLog(String gitRoot, String revision, String filePath) throws Exception
	{
		File rootDir = new File(gitRoot);  
		
        if (new File(gitRoot + File.separator + GIT).exists() == false) {  
            Git.init().setDirectory(rootDir).call();  
        }  
        
        Git git = Git.open(rootDir);
        Repository repo = git.getRepository();
        ObjectId objId = null;
        if (revision != null)
        	objId = repo.resolve(revision);
        else
        	objId = repo.resolve(HEAD);
        Iterable<RevCommit> revCommits = null;
        if (filePath == null)
        	revCommits = git.log().add(objId).call();
        else
        	revCommits = git.log().addPath(filePath).add(objId).call();
        
        GitLogDto logDto = null;
        for (RevCommit commit : revCommits)
    	{
    		logDto = new GitLogDto(commit);
    		break;//否则会取出parent的log
    	}
        
        return logDto;
	}
	
	//获取指定文件的所有log
	public static List<GitLogDto> getLogDtoList(String gitRoot, String filePath) throws Exception
    {
    	List<GitLogDto> rstDtoList = new ArrayList<GitLogDto>();
    	
    	File rootDir = new File(gitRoot);  
		
        Repository repo = new FileRepository(rootDir);
        repo.getConfig().load();
        
        Git git = Git.open(rootDir);
        Iterable<RevCommit> revCommits = git.log().addPath(filePath).call();;
        
        GitLogDto logDto = null;
        for (RevCommit commit : revCommits)
    	{
    		logDto = new GitLogDto(commit);
    		rstDtoList.add(logDto);
    	}
    	
    	return rstDtoList;
    }
	
	//获取版本之间的变更:包含某个目录的变更或具体某个文件的变更信息，具体根据filePath来确定
	public static List<GitDiffStatusDto> getChanges(String gitRoot, String startRevision, String untilRevision, String filePath) throws Exception
	{	
		File rootDir = new File(gitRoot);  
        if (new File(gitRoot + File.separator + GIT).exists() == false) {  
            Git.init().setDirectory(rootDir).call();  
        }  
        
        Git git = Git.open(rootDir);
        Repository repository = git.getRepository();
        
        ObjectReader reader = repository.newObjectReader();  
		CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();  

		ObjectId old = repository.resolve(startRevision + "^{tree}");  
		ObjectId head = repository.resolve(untilRevision+"^{tree}");  
		oldTreeIter.reset(reader, old);  
		CanonicalTreeParser newTreeIter = new CanonicalTreeParser();  
		newTreeIter.reset(reader, head);  
		
		List<DiffEntry> diffs = null;
		if (filePath != null)
		{
			diffs = git.diff().setPathFilter(PathFilter.create(filePath))
						.setNewTree(newTreeIter)  
						.setOldTree(oldTreeIter)  
						.call();
		}
		
		DiffEntry diff = null;
		GitDiffStatusDto diffStatusDto = null;
		List<GitDiffStatusDto> rstList = null;
		if (diffs != null && diffs.size() > 0)
		{	
			rstList = new ArrayList<GitDiffStatusDto>();
			for (int i = 0; i < diffs.size(); i++)
			{
				diff = diffs.get(i);
				if (diff != null)
				{
					diffStatusDto = new GitDiffStatusDto(diff, gitRoot);
					rstList.add(diffStatusDto);
				}
			}
		}
		
		return rstList;
	}
	
	//获取指定文件在两个版本之间的diff
	public static String getDiff(String gitRoot, String revision1, String revision2, String filePath) throws Exception
	{
		File rootDir = new File(gitRoot);  
        if (new File(gitRoot + File.separator + GIT).exists() == false) {  
            Git.init().setDirectory(rootDir).call();  
        }  
        
        Git git = Git.open(rootDir);
        Repository repository = git.getRepository();
        
        ObjectReader reader = repository.newObjectReader();  
		CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();  

		ObjectId old = repository.resolve(revision1 + "^{tree}");  
		ObjectId head = repository.resolve(revision2+"^{tree}");  
		oldTreeIter.reset(reader, old);  
		CanonicalTreeParser newTreeIter = new CanonicalTreeParser();  
		newTreeIter.reset(reader, head);  
		
		List<DiffEntry> diffs = null;
		if (filePath != null)
		{
			diffs = git.diff().setPathFilter(PathFilter.create(filePath))
						.setNewTree(newTreeIter)  
						.setOldTree(oldTreeIter)  
						.call();
		}
        
		String diffText = null;
		if (diffs != null && diffs.size() > 0)
		{	
			ByteArrayOutputStream out = new ByteArrayOutputStream();  
		    DiffFormatter df = new DiffFormatter(out);  
		    df.setRepository(git.getRepository());  
		    df.format(diffs.get(0));
		    diffText = out.toString("gb2312");
		}
		
		return diffText;
	}
	
	//某个文件指定版本的内容
    public static ByteArrayOutputStream getContent(Repository repository, String gitRoot, String revision, String filePath) throws Exception
    //public static byte[] getContent(Repository repository, String gitRoot, String revision, String filePath) throws Exception
    {
		if (repository == null)
		{
			File rootDir = new File(gitRoot);  
	        if (new File(gitRoot + File.separator + GIT).exists() == false) {  
	            Git.init().setDirectory(rootDir).call();  
	        }  
	        
	        Git git = Git.open(rootDir);
	        repository = git.getRepository();
		}
		
		//byte[] bytes = null;
		ByteArrayOutputStream  out = null;
		if (repository != null)
		{
			RevWalk walk = new RevWalk(repository);
			ObjectId objId = null;
			if (revision != null)
				objId = repository.resolve(revision);
			else
				objId = repository.resolve(HEAD);//如果没有版本号，则去HEAD
		    
		    RevCommit revCommit = null;
		    RevTree revTree = null;
		    if (walk != null)
		    {		    	
		    	revCommit = walk.parseCommit(objId);
		    	if (revCommit != null)
			    {		    	
			    	revTree = revCommit.getTree();  
			    }
		    }
		    
		    TreeWalk treeWalk = TreeWalk.forPath(repository, filePath, revTree);//relative file path
		    if (treeWalk == null)  
		        return null;  
		    ObjectId blobId = treeWalk.getObjectId(0);
		    ObjectLoader loader = repository.open(blobId);
		    loader.copyTo(out);
		    /*if (loader != null)
		    	bytes = loader.getBytes();*/
		}
    	
		//return bytes;
		return out;
    }  
	
    //获取当前文件上一个版本的内容
    public static ByteArrayOutputStream getPreRevisionContent(String gitRoot, String revision, String filePath) throws Exception
    {
    	File rootDir = new File(gitRoot);  
        if (new File(gitRoot + File.separator + GIT).exists() == false) {  
            Git.init().setDirectory(rootDir).call();  
        }  
        
        Git git = Git.open(rootDir);
        Repository repository = git.getRepository();
        //获取上一个版本号
        String preVision = getPreviousRevision(null, repository, revision);
        //ByteArrayOutputStream out = getContent(repository, gitRoot, preVision, filePath);
		//byte[] bytes = getContent(repository, gitRoot, preVision, filePath);
    	
        //return out;
        return null;
        //return bytes;
    }
	
  //获取上一个版本号
    public static String getPreviousRevision(String gitRoot, Repository repository, String revision) throws Exception
    {   
    	String preVision = null;
    	if (repository == null)
    	{
    		if (gitRoot != null)
    		{
    			File rootDir = new File(gitRoot);  
    	        if (new File(gitRoot + File.separator + GIT).exists() == false) {  
    	            Git.init().setDirectory(rootDir).call();  
    	        }  
    	        
    	        Git git = Git.open(rootDir);
    	        repository = git.getRepository();
    		}
    	}
    	
    	if (repository != null)
		{
			RevWalk walk = new RevWalk(repository);    
			ObjectId objId = repository.resolve(revision);//通过版本号分解，得到版本对象(String>>>object)
			RevCommit revCommit = null;
			
			if (objId != null)
			{
				revCommit = walk.parseCommit(objId);
				if (revCommit != null)
				{
					preVision = revCommit.getParent(0).getName();//取得上一版本号  
				}
			}
		}
        
        return preVision;
    }
    
	/** 
     * 获取上一版本的变更记录，如果是新增的文件，不会显示，因为做回滚时不需要回滚新增的文件 
     * @param gitRoot git仓库目录 
     * @param revision 版本号 
     * @return 
     * @throws Exception 
     */  
    public static List<DiffEntry> rollBackFile(String gitRoot, String revision) throws Exception 
    {  
        Git git = Git.open(new File(gitRoot));  
        Repository repository = git.getRepository();  
  
        ObjectId objId = repository.resolve(revision);  
        Iterable<RevCommit> allCommitsLater = git.log().add(objId).call();  
        Iterator<RevCommit> iter = allCommitsLater.iterator();  
        RevCommit commit = iter.next();  
        TreeWalk tw = new TreeWalk(repository);  
        tw.addTree(commit.getTree());  
        commit = iter.next();  
        if (commit != null) {  
            tw.addTree(commit.getTree());  
        } else {  
            throw new Exception("当前库只有一个版本，不能获取变更记录");  
        }  
  
        tw.setRecursive(true);  
        RenameDetector rd = new RenameDetector(repository);  
        rd.addAll(DiffEntry.scan(tw));  
        List<DiffEntry> diffEntries = rd.compute();  
        if (diffEntries == null || diffEntries.size() == 0) {  
            return diffEntries;  
        }  
        Iterator<DiffEntry> iterator = new ArrayList<DiffEntry>(diffEntries).iterator();  
        DiffEntry diffEntry = null;  
        while (iterator.hasNext()) {  
            diffEntry = iterator.next();  
            System.out.println("newPath:" + diffEntry.getNewPath() + "    oldPath:"  
                               + diffEntry.getOldPath() + "   changeType:"  
                               + diffEntry.getChangeType());  
            if (diffEntry.getChangeType() == ChangeType.DELETE) {  
                iterator.remove();  
            }  
        }
        
        return diffEntries;  
    }
    
    /** 
     * 回滚到指定版本的上一个版本 
     * @param gitRoot git仓库目录 
     * @param diffEntries 需要回滚的文件 
     * @param revision 版本号 
     * @param remark 备注 
     * @return 
     * @throws Exception 
     */  
    public static boolean rollBackPreRevision(String gitRoot, List<DiffEntry> diffEntries, String revision, String remark) throws Exception
    {  
		if (diffEntries == null || diffEntries.size() == 0) {  
			throw new Exception("没有需要回滚的文件");  
		}  
		
		Git git = Git.open(new File(gitRoot));  
		
		List<String> files = new ArrayList<String>();  
		
		//注意：下面的reset命令会将暂存区的内容恢复到指定（revesion）的状态，相当于取消add命令的操作  
		/*Repository repository = git.getRepository(); 
		
		RevWalk walk = new RevWalk(repository); 
		ObjectId objId = repository.resolve(revision); 
		RevCommit revCommit = walk.parseCommit(objId); 
		String preVision = revCommit.getParent(0).getName(); 
		ResetCommand resetCmd = git.reset(); 
		for (String file : files) { 
		resetCmd.addPath(file); 
		} 
		resetCmd.setRef(preVision).call(); 
		repository.close();*/  
		
		//取出需要回滚的文件，新增的文件不回滚  
		for (DiffEntry diffEntry : diffEntries) 
		{  
			if (diffEntry.getChangeType() == ChangeType.DELETE) 
			{  
				continue;  
			}
			else
			{  
				files.add(diffEntry.getNewPath());  
			}  
		}  
		
		if (files.size() == 0)
		{  
			throw new Exception("没有需要回滚的文件");  
		}  
		
		//checkout操作会丢失工作区的数据，暂存区和工作区的数据会恢复到指定（revision）的版本内容  
		CheckoutCommand checkoutCmd = git.checkout();  
		for (String file : files) 
		{  
			checkoutCmd.addPath(file);  
		}  
		//加了“^”表示指定版本的前一个版本，如果没有上一版本，在命令行中会报错，例如：error: pathspec '4.vm' did not match any file(s) known to git.  
		checkoutCmd.setStartPoint(revision + "^");  
		checkoutCmd.call();  
		
		//重新提交一次  
		CommitCommand commitCmd = git.commit();  
		for (String file : files) 
		{  
			commitCmd.setOnly(file);  
		}
		
		commitCmd.setCommitter("yonge", "654166020@qq.com").setMessage(remark).call();  
		
		return true;  
	} 
	
    //遍历目录
    public static List<GitDirEntryDto> listDirEntries(String gitRoot, String revision, String filePath) throws Exception
    {
    	File rootDir = new File(gitRoot);  
        if (new File(gitRoot + File.separator + GIT).exists() == false) {  
            Git.init().setDirectory(rootDir).call();  
        }  
        
        Git git = Git.open(rootDir);
        Repository repo = git.getRepository();
        
        String fileRootPath = gitRoot + "/" + filePath;
        FileTreeIterator fileIter = new FileTreeIterator(new File(fileRootPath), FS.DETECTED, null);
		
		File file = null;
		List<GitDirEntryDto> rstDtoList = new ArrayList<GitDirEntryDto>();;
		GitDirEntryDto entryDto = null;
		while(fileIter != null)
		{	
			if (!fileIter.eof())
			{	
				entryDto = new GitDirEntryDto();
				file = fileIter.getEntryFile();
				entryDto.name = file.getName();
				entryDto.relativePath = filePath + "/" + file.getName();
				entryDto.url = file.getPath();
				entryDto.repositoryRoot = gitRoot;
				
				if (file.isDirectory())
				{
					entryDto.kind = GitDiffStatusDto.GIT_KIND_DIR;
				}
				else if (file.isFile())
				{
					entryDto.kind = GitDiffStatusDto.GIT_KIND_FILE;
					entryDto.size = file.length();
				}
				else if (file.isHidden())
				{
					entryDto.kind = GitDiffStatusDto.GIT_KIND_HIDDEN;
				}
				else
				{
					entryDto.kind = GitDiffStatusDto.GIT_KIND_UNKNOWN;
				}
				
				rstDtoList.add(entryDto);
				fileIter.next(1);
			}
			else
			{
				fileIter = null;
			}
		}
    	
		//对filePathList中的每一个path获取在revision时候的内容，如果内容为空，那么说明该版本下没有该file,删除掉
		Iterator<GitDirEntryDto> iter = rstDtoList.iterator();
		while (iter.hasNext())
		{
			entryDto = iter.next();
			ByteArrayOutputStream out = getContent(repo, gitRoot, revision, entryDto.relativePath);
			//byte[] bytes = getContent(repo, gitRoot, revision, entryDto.relativePath);
			if (out == null)
			{
				iter.remove();
			}
			else
			{
				if (repo != null)
		    	{
		    		RevWalk walk = new RevWalk(repo);
					ObjectId objId = null;
					if (revision != null)
						objId = repo.resolve(revision);
				    
				    RevCommit revCommit = null;
				    if (walk != null)
				    {		    	
				    	revCommit = walk.parseCommit(objId);
				    	if (revCommit != null)
				    	{
				    		entryDto.commitAuthor = revCommit.getAuthorIdent().getName();
				    		entryDto.commitDate = revCommit.getCommitterIdent().getWhen();
				    		entryDto.commitMessage = revCommit.getFullMessage();
				    	}
				    }
		    	}
			}
		}
		
    	return rstDtoList;
    }
    
    
    public static void cloneRemoteRepository() throws Exception
    {
    	CloneCommand ccmd = Git.cloneRepository();
    	ccmd.setDirectory(new File("d:/ABC"));
    	ccmd.setURI("git@github.com:czw200809/git_repo.git");
    	ccmd.setCloneAllBranches(true);
    	ccmd.call();
    	
    	Repository repo = ccmd.getRepository();
    }
    
    // create a git repository browser with jgit
    public static void repoBrowser(String gitRoot) throws Exception 
    {
    	File directory = new File(gitRoot);
    	Repository repository = RepositoryCache.open(RepositoryCache.FileKey.lenient(directory, FS.DETECTED), true);
    	
    	try
    	{
    	  ObjectId revId = repository.resolve(Constants.HEAD);
    	  DirCache cache = new DirCache(directory, FS.DETECTED);
    	  TreeWalk treeWalk = new TreeWalk(repository);

    	  treeWalk.addTree(new RevWalk(repository).parseTree(revId));
    	  treeWalk.addTree(new DirCacheIterator(cache));

    	  while (treeWalk.next())
    	  {
    	    System.out.println("---------------------------");
    	    System.out.append("name: ").println(treeWalk.getNameString());
    	    System.out.append("path: ").println(treeWalk.getPathString());

    	    ObjectLoader loader = repository.open(treeWalk.getObjectId(0));

    	    System.out.append("directory: ").println(loader.getType()
    	                      == Constants.OBJ_TREE);
    	    System.out.append("size: ").println(loader.getSize());
    	    // ???
    	    System.out.append("last modified: ").println("???");
    	    System.out.append("message: ").println("???");
    	  }
    	}
    	finally
    	{
    	  if (repository != null)
    	  {
    	    repository.close();
    	  }
    	}
    }
    
    public static String remoteBranch(String gitRoot, String branchName, String fileName) throws Exception
    {
    	final Git git = Git.open(new File(gitRoot));  
        Repository repository = git.getRepository();  
  
        repository = git.getRepository();  
        RevWalk walk = new RevWalk(repository);  
        //Ref ref = repository.getRef(branchName);
        Ref ref = null;
        if (ref == null) {  
            //获取远程分支  
            ref = repository.getRef(REF_REMOTES + branchName);  
        }  
        //异步pull  
        ExecutorService executor = Executors.newCachedThreadPool();  
        FutureTask<Boolean> task = new FutureTask<Boolean>(new Callable<Boolean>() {  
            @Override  
            public Boolean call() throws Exception {  
                /*//创建分支 
                CreateBranchCommand createBranchCmd = git.branchCreate(); 
                createBranchCmd.setStartPoint(REF_REMOTES + branchName).setName(branchName).call();*/  
                return git.pull().call().isSuccessful();  
            }  
        });  
        executor.execute(task);  
  
        ObjectId objId = ref.getObjectId();  
        RevCommit revCommit = walk.parseCommit(objId);  
        RevTree revTree = revCommit.getTree();  
  
        TreeWalk treeWalk = TreeWalk.forPath(repository, fileName, revTree);  
  
        ObjectId blobId = treeWalk.getObjectId(0);  
        ObjectLoader loader = repository.open(blobId);  
        byte[] bytes = loader.getBytes();  
        if (bytes != null)  
            return new String(bytes);  
        return null; 
    }
    
    //traverse dir entry
    //每次必须传revision,如果没有传入就是HEAD
    public static List<String> listDirEntry(String gitRoot, String filePath, String revision) throws Exception
    {	
    	File file = new File(gitRoot);
    	final Git git = Git.open(file);
    	Repository repository = git.getRepository();
    	
    	String branch = "refs/heads/branch1";
    	ObjectId branchObjectId = repository.resolve(branch);
    	//ObjectId objId = repository.resolve(revision);
    	RevWalk revWalk = new RevWalk(repository);
		RevCommit commit1 = revWalk.parseCommit(branchObjectId);
		//RevCommit commit = revWalk.parseCommit(objId);
		//RevTree tree = commit.getTree();
		RevTree tree1 = commit1.getTree();
		TreeWalk treeWalk = new TreeWalk(repository);
		//treeWalk.addTree(tree);
		treeWalk.addTree(tree1);
		treeWalk.setRecursive(true);
		treeWalk.setFilter(TreeFilter.ALL);
		
		List<String> pathList = new ArrayList<String>();
		List<String> nameList = new ArrayList<String>();
		while (treeWalk.next()) {
			
			pathList.add(treeWalk.getPathString());
		}
		
		for (Iterator<String> iter = pathList.iterator(); iter.hasNext();)//获取下一级目录的File
		{	
			String path = iter.next();
			int idx = path.indexOf(filePath);
			int secIdx = 0;
			
			String name = null;
			if (idx != -1)
			{
				secIdx = path.indexOf("/", filePath.length() + 1);
				if (secIdx != -1)
					name = path.substring(filePath.length() + 1, secIdx);
				else
					name = path.substring(filePath.length() + 1);
				
				if (!nameList.contains(name))
					nameList.add(name);
			}
		}
		
		pathList.clear();
		
		for (Iterator<String> it = nameList.iterator(); it.hasNext();)
		{
			String name = it.next();
			pathList.add(filePath + "/" + name);
		}
		
		List<GitLogDto> logDtoList = null;
		for (int i = 0; i < pathList.size(); i++)
		{
			//获取某个文件或目录的所有版本log,其中idx == 0的是最新提交版本
			logDtoList = getLogDtoList(gitRoot, pathList.get(i));
		}
		
		return pathList;
    }
    
    /*//list文件目录
	public static List<GitDirEntryDto> listDirEntry(String gitRoot, String revision, String filePath) throws Exception
	{
		List<GitDirEntryDto> rstList = null;
		List<Map<String, Object>> fileEntrys = traverseDirEntry(gitRoot, revision, filePath);
		
		if (fileEntrys != null && fileEntrys.size() > 0)
		{
			rstList = new ArrayList<GitDirEntryDto>();
			GitDirEntryDto entryDto = null;
			for (Iterator<Map<String, Object>> iters = fileEntrys.iterator(); iters.hasNext();)
			{	
				Map<String, Object> fileMap = iters.next();
				
				String fileName = (String)fileMap.get("name");
				String url = (String)fileMap.get("path");
				String relativePath = (String)fileMap.get("relativePath");
				int fileType = (Integer)fileMap.get("fileType");
				long fileSize = (Long)fileMap.get("fileSize");
				List<RevCommit> revs = getRevCommit(gitRoot, relativePath, revision);
				RevCommit rev = revs.get(0);
				
				entryDto = new GitDirEntryDto();
				entryDto.name = fileName;
				entryDto.url = url;
				entryDto.relativePath = relativePath;
				entryDto.kind = fileType;
				entryDto.size = (int)fileSize;
				entryDto.repositoryRoot = gitRoot;
				entryDto.commitAuthor = rev.getAuthorIdent().getName();
				entryDto.commitDate = rev.getCommitterIdent().getWhen();
				entryDto.commitMessage = rev.getFullMessage();
				entryDto.commitRevision = revision;
				
				rstList.add(entryDto);
			}
		}
 		
		return rstList;
	}
    
	//获取某个文件在某个版本的RevCommit
	public static List<RevCommit> getRevCommit(String gitRoot, String filePath, String revision) throws Exception
	{
		File file = new File(gitRoot);
		Git git = Git.open(file);
		Repository repository = git.getRepository();
		
		List<RevCommit> rstList = null;
		Iterable<RevCommit> logs = null;
		
		if (git != null)
		{
			if (revision != null)
			{	
				rstList = new ArrayList<RevCommit>();
				ObjectId objId = repository.resolve(revision);
				logs = git.log().addPath(filePath).add(objId).call();
				
				for (RevCommit revCommit : logs)
				{
					rstList.add(revCommit);
				}
			} 
		}
		
		return rstList;
	}
	
	//遍历目录结构
    public static List<Map<String, Object>> traverseDirEntry(String gitRoot, String revision, String filePath) throws Exception
    {
    	File rootFile = new File(gitRoot + "/" + GIT);
		Git git = Git.open(rootFile);
		Repository repository = git.getRepository();
		String fileRootPath = gitRoot + "/" + filePath;
		
		boolean isTrue = false;
		List<Map<String, Object>> rstList = new ArrayList<Map<String, Object>>();
		if (repository != null)
		{
			FileTreeIterator fileIter = new FileTreeIterator(repository);
			while(fileIter != null)
			{
				if (!fileIter.eof() && rstList.size() <= 0)
				{
					rstList = getFileInfo(fileIter.getEntryFile(), fileRootPath, filePath, isTrue, rstList);
					fileIter.next(1);
				}
				else
				{
					fileIter = null;
				}
			}
		}
		
		return rstList;
    } 
    
    public static List<Map<String, Object>> getFileInfo(File file, String filePath, String relativePath, boolean isTrue, List<Map<String, Object>> rstList) throws Exception
    {
    	File[] files = null;
    	if (file != null)
    	{	
    		if (file.isDirectory())
    		{
    			if (isTrue)
	    		{	
    				Map<String, Object> map = new HashMap<String, Object>();
    				map.put("name", file.getName());
    				map.put("path", file.getPath());
    				map.put("relativePath", (relativePath + "/" + file.getName()).replace("/", "\\"));
    				map.put("fileType", GitDiffStatusDto.GIT_KIND_DIR);
    				map.put("fileSize", 0);
	    			rstList.add(map);
	    			return rstList;
	    		}
	    		
	    		files = file.listFiles();
	    		String path = file.getPath().replace("\\", "/");
	    		if (filePath.equals(path))
	    		{
	    			isTrue = true;    			
	    		}
	    		
	    		for (File subFile : files)
	    		{
	    			getFileInfo(subFile, filePath, relativePath, isTrue, rstList);
	    		}
    		}
    		else if (file.isFile() && isTrue)
    		{	
    			Map<String, Object> map = new HashMap<String, Object>();
				map.put("name", file.getName());
				map.put("path", file.getPath());
				map.put("relativePath", (relativePath + "/" + file.getName()).replace("/", "\\"));
				map.put("fileType", GitDiffStatusDto.GIT_KIND_FILE);
				map.put("fileSize", file.length());
				
    			rstList.add(map);
    		}
    		else 
    		{
    			if (isTrue)
    			{
    				Map<String, Object> map = new HashMap<String, Object>();
    				map.put("name", file.getName());
    				map.put("path", file.getPath());
    				map.put("relativePath", (relativePath + "/" + file.getName()).replace("/", "\\"));
    				map.put("fileType", GitDiffStatusDto.GIT_KIND_NONE);
    				map.put("fileSize", 0);
        			rstList.add(map);
    			}
    		}
    	}
    	
    	return rstList;
    }*/
    
}
