finished:
	1.//获取log,如果revision = null, 则表示获取所有版本的log, 否则获取指定版本的log
		public List<GitLogDto> getLog(String gitRoot, String revision) throws Exception
	2.//比较两个版本之间的差异(某个文件或全部)
		public List<String> getDiffBetweenRevisions(String gitRoot, String Child, String Parent, String filePath) throws Exception
	3.//某个文件指定版本的内容
    	public byte[] getContentWithSpecifiedRevision(Repository repository, String gitRoot, String revision, String filePath) throws Exception
      //获取当前文件上一个版本的内容
    	public byte[] getPreRevisionContent(String gitRoot, String revision, String filePath) throws Exception
