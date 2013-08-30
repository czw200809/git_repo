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
	
  //��ȡĳ���ļ��������ļ����Ӱ汾startRevision��untilRevision��log
    //���untilRevision == null, ��untilRevision = "HEAD"
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
        	
        	//��ȡstartRevision����汾��log,��Ϊǰ��ķ�Χ���ǲ�����startRevision����汾
        	logDto = getSpecificLog(gitRoot, startRevision, filePath);
        	logDtoList.add(logDto);
        }
        
		return logDtoList;
	}
	
	//��ȡָ���汾��log��Ϣ/ָ��File��ָ���汾��log
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
    		break;//�����ȡ��parent��log
    	}
        
        return logDto;
	}
	
	//��ȡָ���ļ�������log
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
	
	//��ȡ�汾֮��ı��:����ĳ��Ŀ¼�ı�������ĳ���ļ��ı����Ϣ���������filePath��ȷ��
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
	
	//��ȡָ���ļ��������汾֮���diff
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
	
	//ĳ���ļ�ָ���汾������
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
				objId = repository.resolve(HEAD);//���û�а汾�ţ���ȥHEAD
		    
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
	
    //��ȡ��ǰ�ļ���һ���汾������
    public static ByteArrayOutputStream getPreRevisionContent(String gitRoot, String revision, String filePath) throws Exception
    {
    	File rootDir = new File(gitRoot);  
        if (new File(gitRoot + File.separator + GIT).exists() == false) {  
            Git.init().setDirectory(rootDir).call();  
        }  
        
        Git git = Git.open(rootDir);
        Repository repository = git.getRepository();
        //��ȡ��һ���汾��
        String preVision = getPreviousRevision(null, repository, revision);
        //ByteArrayOutputStream out = getContent(repository, gitRoot, preVision, filePath);
		//byte[] bytes = getContent(repository, gitRoot, preVision, filePath);
    	
        //return out;
        return null;
        //return bytes;
    }
	
  //��ȡ��һ���汾��
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
			ObjectId objId = repository.resolve(revision);//ͨ���汾�ŷֽ⣬�õ��汾����(String>>>object)
			RevCommit revCommit = null;
			
			if (objId != null)
			{
				revCommit = walk.parseCommit(objId);
				if (revCommit != null)
				{
					preVision = revCommit.getParent(0).getName();//ȡ����һ�汾��  
				}
			}
		}
        
        return preVision;
    }
    
	/** 
     * ��ȡ��һ�汾�ı����¼��������������ļ���������ʾ����Ϊ���ع�ʱ����Ҫ�ع��������ļ� 
     * @param gitRoot git�ֿ�Ŀ¼ 
     * @param revision �汾�� 
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
            throw new Exception("��ǰ��ֻ��һ���汾�����ܻ�ȡ�����¼");  
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
     * �ع���ָ���汾����һ���汾 
     * @param gitRoot git�ֿ�Ŀ¼ 
     * @param diffEntries ��Ҫ�ع����ļ� 
     * @param revision �汾�� 
     * @param remark ��ע 
     * @return 
     * @throws Exception 
     */  
    public static boolean rollBackPreRevision(String gitRoot, List<DiffEntry> diffEntries, String revision, String remark) throws Exception
    {  
		if (diffEntries == null || diffEntries.size() == 0) {  
			throw new Exception("û����Ҫ�ع����ļ�");  
		}  
		
		Git git = Git.open(new File(gitRoot));  
		
		List<String> files = new ArrayList<String>();  
		
		//ע�⣺�����reset����Ὣ�ݴ��������ݻָ���ָ����revesion����״̬���൱��ȡ��add����Ĳ���  
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
		
		//ȡ����Ҫ�ع����ļ����������ļ����ع�  
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
			throw new Exception("û����Ҫ�ع����ļ�");  
		}  
		
		//checkout�����ᶪʧ�����������ݣ��ݴ����͹����������ݻ�ָ���ָ����revision���İ汾����  
		CheckoutCommand checkoutCmd = git.checkout();  
		for (String file : files) 
		{  
			checkoutCmd.addPath(file);  
		}  
		//���ˡ�^����ʾָ���汾��ǰһ���汾�����û����һ�汾�����������лᱨ�����磺error: pathspec '4.vm' did not match any file(s) known to git.  
		checkoutCmd.setStartPoint(revision + "^");  
		checkoutCmd.call();  
		
		//�����ύһ��  
		CommitCommand commitCmd = git.commit();  
		for (String file : files) 
		{  
			commitCmd.setOnly(file);  
		}
		
		commitCmd.setCommitter("yonge", "654166020@qq.com").setMessage(remark).call();  
		
		return true;  
	} 
	
    //����Ŀ¼
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
    	
		//��filePathList�е�ÿһ��path��ȡ��revisionʱ������ݣ��������Ϊ�գ���ô˵���ð汾��û�и�file,ɾ����
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
            //��ȡԶ�̷�֧  
            ref = repository.getRef(REF_REMOTES + branchName);  
        }  
        //�첽pull  
        ExecutorService executor = Executors.newCachedThreadPool();  
        FutureTask<Boolean> task = new FutureTask<Boolean>(new Callable<Boolean>() {  
            @Override  
            public Boolean call() throws Exception {  
                /*//������֧ 
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
    //ÿ�α��봫revision,���û�д������HEAD
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
		
		for (Iterator<String> iter = pathList.iterator(); iter.hasNext();)//��ȡ��һ��Ŀ¼��File
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
			//��ȡĳ���ļ���Ŀ¼�����а汾log,����idx == 0���������ύ�汾
			logDtoList = getLogDtoList(gitRoot, pathList.get(i));
		}
		
		return pathList;
    }
    
    /*//list�ļ�Ŀ¼
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
    
	//��ȡĳ���ļ���ĳ���汾��RevCommit
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
	
	//����Ŀ¼�ṹ
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
