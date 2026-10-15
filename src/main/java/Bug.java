import java.util.Date;

public class Bug {

    private int id;
    private String bugName;
    private String description;
    private String bugType;
    private String priority;
    private String bugLevel;
    private String projectName;
    private Date bugDate;
    private String status;
    private String assignedTo;
    private String reportedBy;
    private String screenshotPath;

    public Bug(int id, String bugName, String description, String bugType, String priority,
               String bugLevel, String projectName, Date bugDate, String status,
               String assignedTo, String reportedBy, String screenshotPath) {
        this.id = id;
        this.bugName = bugName;
        this.description = description;
        this.bugType = bugType;
        this.priority = priority;
        this.bugLevel = bugLevel;
        this.projectName = projectName;
        this.bugDate = bugDate;
        this.status = status;
        this.assignedTo = assignedTo;
        this.reportedBy = reportedBy;
        this.screenshotPath = screenshotPath;
    }

    // ── Getters ──
    public int getId()                  { return id; }
    public String getBugName()          { return bugName; }
    public String getDescription()      { return description; }
    public String getBugType()          { return bugType; }
    public String getPriority()         { return priority; }
    public String getBugLevel()         { return bugLevel; }
    public String getProjectName()      { return projectName; }
    public Date getBugDate()            { return bugDate; }
    public String getStatus()           { return status; }
    public String getAssignedTo()       { return assignedTo; }
    public String getReportedBy()       { return reportedBy; }
    public String getScreenshotPath()   { return screenshotPath; }

    // ── Setters ──
    public void setBugName(String bugName)              { this.bugName = bugName; }
    public void setDescription(String description)      { this.description = description; }
    public void setBugType(String bugType)              { this.bugType = bugType; }
    public void setPriority(String priority)            { this.priority = priority; }
    public void setBugLevel(String bugLevel)            { this.bugLevel = bugLevel; }
    public void setProjectName(String projectName)      { this.projectName = projectName; }
    public void setBugDate(Date bugDate)                { this.bugDate = bugDate; }
    public void setStatus(String status)                { this.status = status; }
    public void setAssignedTo(String assignedTo)        { this.assignedTo = assignedTo; }
    public void setReportedBy(String reportedBy)        { this.reportedBy = reportedBy; }
    public void setScreenshotPath(String screenshotPath){ this.screenshotPath = screenshotPath; }

    // ── Print full bug details ──
    public void printBug() {
        System.out.println("------ Bug Details ------");
        System.out.println("ID:               " + id);
        System.out.println("Bug Name:         " + bugName);
        System.out.println("Description:      " + description);
        System.out.println("Bug Type:         " + bugType);
        System.out.println("Priority:         " + priority);
        System.out.println("Bug Level:        " + bugLevel);
        System.out.println("Project Name:     " + projectName);
        System.out.println("Bug Date:         " + bugDate);
        System.out.println("Status:           " + status);
        System.out.println("Assigned To:      " + assignedTo);
        System.out.println("Reported By:      " + reportedBy);
        System.out.println("Screenshot Path:  " + screenshotPath);
        System.out.println("-------------------------");
    }

    // ── Short summary used when printing in lists ──
    @Override
    public String toString() {
        return "Bug #" + id + " | " + bugName + " | Priority: " + priority
             + " | Status: " + status + " | Assigned to: " + assignedTo
             + " | Reported by: " + reportedBy;
    }
}