import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.Scanner;

public class Screenshot {

    // ─────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────
    private static final String[] ALLOWED_EXTENSIONS = {"png", "jpg", "jpeg", "gif", "bmp"};
    private static final String EXTENSION_DESCRIPTION = "Image Files (*.png, *.jpg, *.jpeg, *.gif, *.bmp)";

    // ─────────────────────────────────────────────
    // Public Method: Attach Screenshot
    // ─────────────────────────────────────────────
    public void attachScreenshot(Scanner scanner) {

        // ── Step 1: Get Bug ID ──────────────────
        Bug targetBug = getBugFromUser(scanner);
        if (targetBug == null) return;

        // ── Step 2: Open File Chooser ───────────
        File selectedFile = openFileChooser();
        if (selectedFile == null) {
            System.out.println("❌ Screenshot selection cancelled.");
            return;
        }

        // ── Step 3: Validate File ───────────────
        if (!isValidImageFile(selectedFile)) {
            System.out.println("❌ Invalid file type. Please select a valid image file.");
            return;
        }

        // ── Step 4: Attach Screenshot ───────────
        attachScreenshotToBug(targetBug, selectedFile.getAbsolutePath());
        System.out.println("✅ Screenshot attached successfully!");
        System.out.println("   Bug ID   : " + targetBug.getId());
        System.out.println("   File     : " + selectedFile.getName());
        System.out.println("   Location : " + selectedFile.getAbsolutePath());
    }

    // ─────────────────────────────────────────────
    // Step 1: Read and Validate Bug ID from User
    // ─────────────────────────────────────────────
    private Bug getBugFromUser(Scanner scanner) {

        System.out.print("\nEnter Bug ID: ");
        String input = scanner.nextLine().trim();

        // ── Check Empty Input ───────────────────
        if (input.isEmpty()) {
            System.out.println("❌ Bug ID cannot be empty.");
            return null;
        }

        // ── Parse Integer ───────────────────────
        int bugID;
        try {
            bugID = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid Bug ID. Please enter a numeric value.");
            return null;
        }

        // ── Check Positive ID ───────────────────
        if (bugID <= 0) {
            System.out.println("❌ Bug ID must be a positive number.");
            return null;
        }

        // ── Search for Bug ──────────────────────
        return findBugById(bugID);
    }

    // ─────────────────────────────────────────────
    // Step 2: Open GUI File Chooser
    // ─────────────────────────────────────────────
    private File openFileChooser() {

        // ── Run on Event Dispatch Thread ────────
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Screenshot");

        // ── Filter: Image Files Only ────────────
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                EXTENSION_DESCRIPTION, ALLOWED_EXTENSIONS
        );
        fileChooser.setFileFilter(filter);
        fileChooser.setAcceptAllFileFilterUsed(false); // Hide "All Files" option

        // ── Open Dialog ─────────────────────────
        int result = fileChooser.showOpenDialog(null);

        return (result == JFileChooser.APPROVE_OPTION)
                ? fileChooser.getSelectedFile()
                : null;
    }

    // ─────────────────────────────────────────────
    // Step 3: Validate Image File
    // ─────────────────────────────────────────────
    private boolean isValidImageFile(File file) {

        // ── Check File Exists ───────────────────
        if (!file.exists()) {
            System.out.println("❌ File does not exist.");
            return false;
        }

        // ── Check File Size (Max 10MB) ──────────
        long maxSize = 10 * 1024 * 1024; // 10 MB in bytes
        if (file.length() > maxSize) {
            System.out.println("❌ File size exceeds 10MB limit.");
            return false;
        }

        // ── Check File Extension ────────────────
        String fileName = file.getName().toLowerCase();
        for (String ext : ALLOWED_EXTENSIONS) {
            if (fileName.endsWith("." + ext)) {
                return true;
            }
        }

        return false;
    }

    // ─────────────────────────────────────────────
    // Helper: Find Bug by ID
    // ─────────────────────────────────────────────
    private Bug findBugById(int bugId) {

        for (Bug b : BugService.getAllBugs()) {
            if (b.getId() == bugId) {
                return b;
            }
        }

        System.out.println("❌ Bug with ID " + bugId + " not found.");
        return null;
    }

    // ─────────────────────────────────────────────
    // Helper: Attach Screenshot Path to Bug
    // ─────────────────────────────────────────────
    private void attachScreenshotToBug(Bug bug, String filePath) {
        bug.setScreenshotPath(filePath);
        BugService.saveBugsToFile();
    }
}