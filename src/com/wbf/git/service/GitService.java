package com.wbf.git.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import com.wbf.git.dto.GitDiffStatusDto;
import com.wbf.git.dto.GitDirEntryDto;
import com.wbf.git.dto.GitLogDto;

public class GitService 
{	
	private final static String GIT = ".git";
	private final static String HEAD = "HEAD";
    private final static String REF_REMOTES = "refs/remotes/origin/";
    
    public static Map<String, Object> getGit(String gitRoot, String branchName)
    {	
    	Map<String, Object> rstMap = new HashMap<String, Object>();
    	long sysTime = System.currentTimeMillis();
    	String dirStr = System.getProperty("java.io.tmpdir") + "tmp" + sysTime;
    	File tmpDir = new File(System.getProperty("java.io.tmpdir"), "tmp" + sysTime);
		tmpDir.mkdirs();
		
		Git git = null;
		try {
			git = Git.cloneRepository()
					.setBare(true)
					.setBranch(branchName)
					.setDirectory(tmpDir)
					.setURI(gitRoot)
					.setProgressMonitor(new TextProgressMonitor())
					.call();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		rstMap.put("dirStr", dirStr);
		rstMap.put("git", git);
		
		return rstMap;
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
    
    //获取指定分支(且指定目录)的所有log
    public static List<GitLogDto> getLog(Map<String, Object> rstMap, String gitRoot, String branchName, String filePath)
    {
    	boolean isNew = false;
    	if (rstMap == null)
    	{
    		rstMap = getGit(gitRoot, branchName);
    		isNew = true;
    	}
    	
        Git git = (Git)rstMap.get("git");
        List<GitLogDto> logDtoList = null;
        if (git != null)
        {
        	 Iterable<RevCommit> revCommits = null;        	 
        	 try {
        		 
        		 if (filePath == null)
        			 revCommits = git.log().call();
        		 else
        			 revCommits = git.log().addPath(filePath).call();
        		 
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
        		 }
        	 }catch (Exception e) {
        		 e.printStackTrace();
        	 }finally {
        		         		 
        		 if (isNew == true)
        		 {
        			 git.getRepository().close();
        			 del(new File((String)rstMap.get("dirStr")));
        		 }
        	 }
        }       
        
    	return logDtoList;
    }
    
    //获取指定分支在某个时间段内的log
    public static List<GitLogDto> getLog(Map<String, Object> rstMap, String gitRoot, String branchName, Date startDate, Date untilDate, String filePath)
    {
        List<GitLogDto> logDtoList = getLog(rstMap, gitRoot, branchName, filePath);
        
        for (Iterator<GitLogDto> iter = logDtoList.iterator();iter.hasNext();)
        {
        	GitLogDto logDto = iter.next();
        	Date d = logDto.commitDate;
        	long time = d.getTime();
        	long startTime = startDate.getTime();
        	long untilTime = untilDate.getTime();
        	
        	if (time < startTime && time > untilTime)
        	{
        		iter.remove();
        	}
        }
        
    	return logDtoList;
    }
    
    //获取指定分支在startRev-untilRev之间的版本
    public static List<GitLogDto> getLog(String gitRoot, String branchName, String startRev, String untilRev, String filePath)
    {
    	Map<String, Object> rstMap = getGit(gitRoot, branchName);
    	
    	Git git = null;
    	if (rstMap != null)
    	{
    		git = (Git)rstMap.get("git");
    	}
        
        Repository repo = null;
        List<GitLogDto> logDtoList = null;
        if (git != null)
        {	
        	repo = git.getRepository();
			RevWalk revWalk = new RevWalk(repo);
			RevCommit rev1 = null;
			RevCommit rev2 = null;
			try {
				ObjectId startObjId = null;
				ObjectId untilObjId = null;
				if (startRev != null && untilObjId != null)
				{
					startObjId = repo.resolve(startRev);
					untilObjId = repo.resolve(untilRev);
					rev1 = revWalk.parseCommit(startObjId);
					rev2 = revWalk.parseCommit(untilObjId);
				}
				else
				{
					untilObjId = repo.resolve(Constants.HEAD);
					rev2 = revWalk.parseCommit(untilObjId);
					if (rev2 != null)
						rev1 = rev2.getParent(0);
				}
				
				if (rev1 != null && rev2 != null)
				{
					Date startDate = rev1.getCommitterIdent().getWhen();
					Date untilDate = rev2.getCommitterIdent().getWhen();
					 
					logDtoList = getLog(rstMap, gitRoot, branchName, startDate, untilDate, filePath);
				}
				
			}catch (Exception e) {
				e.printStackTrace();
			}finally {
				if (repo != null)
				{
					repo.close();
					del(new File((String)rstMap.get("dirStr")));					
				}
			}
        }
       
    	return logDtoList;
    }
	
    public static String getDiff(String gitRoot, String branchName, String rev1, String rev2, String filePath)
	{
		Map<String, Object> rstMap = getGit(gitRoot, branchName);
		Git git = null;
    	if (rstMap != null)
    	{
    		git = (Git)rstMap.get("git");
    	}
    	
        Repository repository = null;
        String diffText = null;
        if (git != null)
        {
        	repository = git.getRepository();
            
            RevWalk rw = new RevWalk(repository);
            ObjectId objId1 = null;
            ObjectId objId2 = null;
            RevCommit rc1 = null;
            RevCommit rc2 = null;
            
            try {
            	
            	if (rev1 != null && rev2 != null)
                {
                	objId1 = repository.resolve(rev1);
                	rc1 = rw.parseCommit(objId1);
                	objId2 = repository.resolve(rev2);
                	rc2 = rw.parseCommit(objId2);
                }
                else
                {
                	objId1 = repository.resolve(Constants.HEAD);
                	rc1 = rw.parseCommit(objId1);
                	objId2 = rc1.getParent(0).getId();
                	rc2 = rw.parseCommit(objId2);
                }
                
                ByteArrayOutputStream out = new ByteArrayOutputStream();  
        	    DiffFormatter df = new DiffFormatter(out); 
                df.setRepository(repository);
                df.setPathFilter(PathFilter.create(filePath));
                df.setDiffComparator(RawTextComparator.DEFAULT);
                df.setDetectRenames(true);
                List<DiffEntry> diffs = df.scan(rc1.getTree(), rc2.getTree());
                
                for (DiffEntry diff : diffs) {
                	df.format(diff);
                	diffText = out.toString("utf-8");
                	//System.out.println(diffText);
                    //System.out.println(MessageFormat.format("({0} {1} {2}", diff.getChangeType().name(), diff.getNewMode().getBits(), diff.getNewPath()));
                }
            	
            }catch (Exception e) {
            	e.printStackTrace();
            }finally {
            	
            	 if (repository != null)
            	 {            		 
            		 repository.close();
            		 del(new File((String)rstMap.get("dirStr")));
            	 }
            }  
        }
        
		return diffText;
	}
    
	//获取版本之间的变更:包含某个目录的变更或具体某个文件的变更信息，具体根据filePath来确定
	public static List<GitDiffStatusDto> getChanges(String gitRoot, String branchName, String rev1, String rev2, String filePath)
	{	
		Map<String, Object> rstMap = getGit(gitRoot, branchName);
		
		Git git = null;
    	if (rstMap != null)
    	{
    		git = (Git)rstMap.get("git");
    	}
        
        Repository repository = null;
        List<GitDiffStatusDto> rstList = null;
        if (git != null)
        {
        	repository = git.getRepository();
            
            ObjectReader reader = repository.newObjectReader();  
    		CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();  
    		
    		ObjectId old = null;
    		ObjectId head = null;
    		
    		try {
    			
    			if (rev1 != null && rev2 != null)
        		{
        			old = repository.resolve(rev1 + "^{tree}");  
        			head = repository.resolve(rev2+"^{tree}");
        		}
        		else
        		{
        			String rev = null;
        			if (rev1 != null)
        				rev = rev1;
        			else
        				rev = HEAD;
        			
        			old = repository.resolve(rev + "^{tree}");
        			head = repository.resolve(rev + "^^{tree}");
        			/*old = repository.resolve(HEAD + "^{tree}");
        			head = repository.resolve(HEAD + "^^{tree}");*/
        		}
        		
        		oldTreeIter.reset(reader, old);  
        		CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
        		if (head == null)
        			newTreeIter.reset(reader, old);
        		else
        			newTreeIter.reset(reader, head);  
        		
        		List<DiffEntry> diffs = null;
        		if (filePath != null)
        		{
        			diffs = git.diff()
        					   .setPathFilter(PathFilter.create(filePath))
        					   .setNewTree(newTreeIter)  
        					   .setOldTree(oldTreeIter)  
        					   .call();
        		}
        		
        		GitDiffStatusDto diffStatusDto = null;
        		DiffEntry diff = null;
        		if (diffs != null && diffs.size() > 0)
        		{	
        			rstList = new ArrayList<GitDiffStatusDto>();
        			for (Iterator<DiffEntry> iter = diffs.iterator(); iter.hasNext();)
        			{
        				diff = iter.next();
        				diffStatusDto = new GitDiffStatusDto(diff, gitRoot);
        				rstList.add(diffStatusDto);
        			}
        		}
    		}catch (Exception e) {
    			e.printStackTrace();
    		}finally {
    			
    			if (repository != null)
    			{    				
    				repository.close();
    				del(new File((String)rstMap.get("dirStr")));
    			}
        		
    		}
        }
        
		return rstList;
	}
	
	//某个文件指定版本的内容
    public static byte[] getContent(Map<String, Object> rstMap, String gitRoot, String branchName, String revision, String filePath, Map<String, Object> fileMap)
    {
    	boolean isNew = false;
    	if (rstMap == null)
    	{    		
    		rstMap = getGit(gitRoot, branchName);
    		isNew = true;
    	}
    	
        Git git = null;
    	if (rstMap != null)
    	{
    		git = (Git)rstMap.get("git");
    	}
    	
        Repository repository = null;
        byte[] bytes = null;
        if (git != null)
        {
        	repository = git.getRepository();
    		
    		//ByteArrayOutputStream  out = null;
    		if (repository != null)
    		{
    			RevWalk walk = new RevWalk(repository);
    			ObjectId objId = null;
    			try {
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
	    		    if (fileMap != null)
	    		    {
	    		    	FileMode fileMode = treeWalk.getFileMode(0);
	    		    	int kind = GitDiffStatusDto.getKind(fileMode);
	    		    	fileMap.put("fileType", kind);
	    		    }
	    		    
	    		    if (treeWalk == null)  
	    		        return null;  
	    		    ObjectId blobId = treeWalk.getObjectId(0);
	    		    ObjectLoader loader = repository.open(blobId);
	    		    //loader.copyTo(out);
	    		    if (loader != null)
	    		    	bytes = loader.getBytes();
    			}catch (Exception e) {
    				e.printStackTrace();
    			}finally {
    				if (isNew)
    	    		{
    	    			repository.close();
    	    			del(new File((String)rstMap.get("dirStr")));
    	    		}
    			}	
    		}
        }
        
		return bytes;
		//return out;
    }  
    
    public static List<GitDirEntryDto> listDirEntry(String gitRoot, String branchName, String revision, String filePath)
    {
    	Map<String, Object> rstMap = getGit(gitRoot, branchName);
    	
    	Git git = null;
    	Repository repository = null;
    	if (rstMap != null)
    	{
    		git = (Git)rstMap.get("git");
    		if (git != null)
    		{
    			repository = git.getRepository();
    		}
    	}
        
        List<GitDirEntryDto> rstDtoList = null;
        ObjectId objId = null;
        
        try {
        	
        	 if (revision == null)
             	objId = repository.resolve(Constants.HEAD);
             else
             	objId = repository.resolve(revision);

     		RevWalk walk = new RevWalk(repository);

     		RevCommit commit = walk.parseCommit(objId);
     		RevTree tree = commit.getTree();
     		
     		TreeWalk treeWalk = new TreeWalk(repository);
     		if (filePath != null)
     			treeWalk.setFilter(PathFilter.create(filePath));
     		treeWalk.addTree(tree);
     		//treeWalk.setRecursive(true);
     		
     		int count = 0;
     		String path = null;
     		List<String> pathList = new ArrayList<String>();
     		List<String> nameList = new ArrayList<String>();
     		while(treeWalk.next()) 
     		{	
     			path = treeWalk.getPathString();
     			if (filePath != null)
     			{				
     				if (count > 0 && (path.indexOf(filePath) != -1))
     				{
     					if (!path.equals(filePath))
     					{
     						nameList.add(treeWalk.getNameString());
     						pathList.add(treeWalk.getPathString());
     					}
     				}
     				
     				if (count <= 0 || path.indexOf(filePath) == -1 || path.equals(filePath))
     					treeWalk.enterSubtree();
     				count++;
     			}
     			else
     			{
     				nameList.add(treeWalk.getNameString());
     				pathList.add(treeWalk.getPathString());
     			}
     		}
     		
     		rstDtoList = listDirEntry(rstMap, gitRoot, branchName, revision, pathList, nameList);
        	
        }catch (Exception e) {
        	e.printStackTrace();
        } finally {
        	
        	if (repository != null)
    		{
    			repository.close();
    			del(new File((String)rstMap.get("dirStr")));
    		}
        }
        
        return rstDtoList;
    }
    
    private static List<GitDirEntryDto> listDirEntry(Map<String, Object> rstMap, String gitRoot, String branchName, String revision, List<String> pathList, List<String> nameList)
    {
    	List<GitDirEntryDto> rstDtoList = null;
    	GitDirEntryDto entryDto = null;
    	GitLogDto logDto = null;
    	
    	Repository repo = ((Git)rstMap.get("git")).getRepository();
    	RevWalk rw = new RevWalk(repo);
    	
    	String filePath = null;
    	List<GitLogDto> logDtoList = null;
    	if (pathList != null)
    	{
    		rstDtoList = new ArrayList<GitDirEntryDto>();
    		for (int i = 0; i < pathList.size(); i++)
    		{
    			filePath = pathList.get(i);
    			
    			ObjectId objId = null;
    			try {
    				if (revision == null)
        				objId = repo.resolve(Constants.HEAD);
        			else
        				objId = repo.resolve(revision);
        			
                	RevCommit rev = rw.parseCommit(objId);
                	logDtoList = getLog(rstMap, gitRoot, branchName, filePath);
                	
                	for (Iterator<GitLogDto> it = logDtoList.iterator(); it.hasNext();)
                	{
                		GitLogDto dto = it.next();
                		if (dto.commitDate.getTime() > rev.getCommitterIdent().getWhen().getTime())
                		{
                			it.remove();
                		}
                	}
    			}catch (Exception e) {
    				e.printStackTrace();
    			}
    			
            	entryDto = new GitDirEntryDto();
            	entryDto.name = nameList.get(i);
            	entryDto.url = gitRoot + "/" + filePath;
        		entryDto.repositoryRoot = gitRoot;
        		entryDto.relativePath = filePath;
            	if (logDtoList != null && logDtoList.size() > 0)
            	{	
            		logDto = logDtoList.get(0);
            		entryDto.commitAuthor = logDto.author;
        			entryDto.commitDate = logDto.commitDate;
        			entryDto.commitMessage = logDto.commitMessage;
        			entryDto.commitRevision = logDto.revision;
            	}
            	
            	//如果这个File只有第一次提交，那么是没有parentRev的
            	Map<String, Object> fileMap = new HashMap<String, Object>();
        		fileMap.put("fileType", -1);
        		byte[] bytes = getContent(rstMap, null, null, revision, filePath, fileMap);
        		if (bytes != null)
        		{
        			int kind = (Integer)fileMap.get("fileType");
        			entryDto.kind = kind;
        			if (kind == 6)
        				entryDto.size = bytes.length;
        		}
            	
    			rstDtoList.add(entryDto);
    		}
    	}
    	
    	return rstDtoList;
    }
    
    public String getPreRevision(String gitRoot, String branchName, String revision)
    {
    	Map<String, Object> rstMap = getGit(gitRoot, branchName);
    	
    	String preRev = null;
    	Git git = null;
    	if (rstMap != null)
    		git = (Git)rstMap.get("git");
        
        Repository repo = null;
        if (git != null)
        {	
        	repo = git.getRepository();
			RevWalk revWalk = new RevWalk(repo);
			RevCommit rev1 = null;
			RevCommit rev2 = null;
			try {
				
				ObjectId objId = null;
				objId = repo.resolve(revision);
				rev1 = revWalk.parseCommit(objId);
				if (rev1 != null)
					rev2 = rev1.getParent(0);
				preRev = rev2.getName();
				
			}catch (Exception e) {
				e.printStackTrace();
			}finally {
				if (repo != null)
				{
					repo.close();
					del(new File((String)rstMap.get("dirStr")));					
				}
			}
        }
        
        return preRev;
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
    
    
    
}
