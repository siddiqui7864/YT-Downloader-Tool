import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;

public class YTDownloader extends JFrame {
    private JTextField urlField;
    private JButton downloadVideoButton, downloadMP3Button, pasteButton, darkModeButton;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JPanel inputPanel, buttonPanel, statusPanel;
    private boolean isDarkMode = false;
    private Process currentProcess;
    private String currentOutputPath;

    public YTDownloader() {
        setTitle("YouTube Downloader");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Color.WHITE);

        urlField = new JTextField(40) {
            @Override
            public void setText(String t) {
                if (t.length() > 2048) t = t.substring(0, 2048);
                super.setText(t);
            }
        };
        urlField.setText("Enter YouTube URL");
        urlField.setForeground(Color.GRAY);
        urlField.setCaretColor(Color.BLACK);
        urlField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (urlField.getText().equals("Enter YouTube URL")) {
                    urlField.setText("");
                    urlField.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
                    urlField.setCaretColor(isDarkMode ? Color.WHITE : Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (urlField.getText().isEmpty()) {
                    urlField.setText("Enter YouTube URL");
                    urlField.setForeground(Color.GRAY);
                    urlField.setCaretColor(isDarkMode ? Color.WHITE : Color.BLACK);
                }
            }
        });

        downloadVideoButton = new JButton("Download Video");
        downloadMP3Button = new JButton("Download MP3");
        pasteButton = new JButton("↙");
        darkModeButton = new JButton("Toggle Dark Mode");
        statusLabel = new JLabel("Status: Ready", SwingConstants.CENTER);
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setValue(0);
        progressBar.setString("0%");
        progressBar.setForeground(Color.CYAN);

        inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel urlWithPaste = new JPanel(new BorderLayout(5, 0));
        urlWithPaste.add(urlField, BorderLayout.CENTER);
        urlWithPaste.add(pasteButton, BorderLayout.EAST);
        inputPanel.add(urlWithPaste, BorderLayout.CENTER);

        buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.add(downloadVideoButton);
        buttonPanel.add(downloadMP3Button);
        buttonPanel.add(darkModeButton);

        statusPanel = new JPanel(new BorderLayout(5, 5));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        statusPanel.add(statusLabel, BorderLayout.NORTH);
        statusPanel.add(progressBar, BorderLayout.CENTER);

        add(inputPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);

        pasteButton.addActionListener(e -> {
            java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
            try {
                String clipboardContent = (String) clipboard.getData(java.awt.datatransfer.DataFlavor.stringFlavor);
                urlField.setText(clipboardContent);
            } catch (Exception ex) {
                statusLabel.setText("Status: Failed to paste from clipboard");
            }
        });

        darkModeButton.addActionListener(e -> {
            isDarkMode = !isDarkMode;
            Color bgColor = isDarkMode ? Color.DARK_GRAY : Color.WHITE;
            Color fgColor = isDarkMode ? Color.WHITE : Color.BLACK;
            Color fieldBgColor = isDarkMode ? new Color(50, 50, 50, 200) : Color.WHITE;
            Color buttonBgColor = isDarkMode ? new Color(80, 80, 80, 200) : new Color(200, 200, 200, 200);
            Color progressBgColor = isDarkMode ? new Color(50, 50, 50, 200) : Color.LIGHT_GRAY;

            getContentPane().setBackground(bgColor);
            urlField.setBackground(fieldBgColor);
            urlField.setForeground(fgColor);
            urlField.setCaretColor(fgColor);
            if (urlField.getText().equals("Enter YouTube URL")) urlField.setForeground(Color.GRAY);
            downloadVideoButton.setBackground(buttonBgColor);
            downloadVideoButton.setForeground(fgColor);
            downloadMP3Button.setBackground(buttonBgColor);
            downloadMP3Button.setForeground(fgColor);
            pasteButton.setBackground(buttonBgColor);
            pasteButton.setForeground(fgColor);
            darkModeButton.setBackground(buttonBgColor);
            darkModeButton.setForeground(fgColor);
            statusLabel.setForeground(fgColor);
            progressBar.setBackground(progressBgColor);
            inputPanel.setBackground(bgColor);
            buttonPanel.setBackground(bgColor);
            statusPanel.setBackground(bgColor);
            statusLabel.setText("Status: Dark mode " + (isDarkMode ? "enabled" : "disabled"));
        });

        downloadVideoButton.addActionListener(e -> {
            String url = urlField.getText().trim();
            if (url.isEmpty() || url.equals("Enter YouTube URL")) {
                statusLabel.setText("Status: Please enter a valid URL");
                progressBar.setValue(0);
                progressBar.setString("0%");
                return;
            }
            boolean isPlaylist = url.contains("list=");
            currentOutputPath = isPlaylist ? "Downloads/Videos/Playlist/%(playlist_title)s/%(title)s.%(ext)s" : "Downloads/Videos/%(title)s.%(ext)s";
            statusLabel.setText("Status: Downloading " + (isPlaylist ? "playlist..." : "video..."));
            progressBar.setValue(0);
            progressBar.setString("0%");
            Thread downloadThread = new Thread(() -> {
                try {
                    new File("Downloads/Videos/Playlist").mkdirs();
                    ProcessBuilder pb = new ProcessBuilder(
                            "yt-dlp",
                            isPlaylist ? "--yes-playlist" : "--no-playlist",
                            "-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/mp4",
                            url,
                            "-o", currentOutputPath
                    );
                    pb.directory(new File("."));
                    pb.redirectErrorStream(true);
                    currentProcess = pb.start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()));
                    String line;
                    Pattern progressPattern = Pattern.compile("\\[download\\]\\s+(\\d+\\.\\d+)%");
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                        Matcher matcher = progressPattern.matcher(line);
                        if (matcher.find()) {
                            float progress = Float.parseFloat(matcher.group(1));
                            int progressValue = (int) progress;
                            SwingUtilities.invokeLater(() -> {
                                progressBar.setValue(progressValue);
                                progressBar.setString(progressValue + "%");
                            });
                        }
                    }
                    int exitCode = currentProcess.waitFor();
                    SwingUtilities.invokeLater(() -> {
                        if (exitCode == 0) {
                            statusLabel.setText("Status: " + (isPlaylist ? "Playlist" : "Video") + " downloaded to Downloads/Videos" + (isPlaylist ? "/Playlist" : "") + "!");
                            progressBar.setValue(100);
                            progressBar.setString("100%");
                        } else {
                            statusLabel.setText("Status: Download failed. Check console.");
                            progressBar.setValue(0);
                            progressBar.setString("0%");
                        }
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Status: Error: " + ex.getMessage());
                        progressBar.setValue(0);
                        progressBar.setString("0%");
                    });
                }
            });
            downloadThread.start();
        });

        downloadMP3Button.addActionListener(e -> {
            String url = urlField.getText().trim();
            if (url.isEmpty() || url.equals("Enter YouTube URL")) {
                statusLabel.setText("Status: Please enter a valid URL");
                progressBar.setValue(0);
                progressBar.setString("0%");
                return;
            }
            boolean isPlaylist = url.contains("list=");
            currentOutputPath = isPlaylist ? "Downloads/Music/%(playlist_title)s/%(title)s.%(ext)s" : "Downloads/Music/%(title)s.%(ext)s";
            statusLabel.setText("Status: Downloading " + (isPlaylist ? "playlist as MP3..." : "video as MP3..."));
            progressBar.setValue(0);
            progressBar.setString("0%");
            Thread downloadThread = new Thread(() -> {
                try {
                    new File("Downloads/Music").mkdirs();
                    ProcessBuilder pb = new ProcessBuilder(
                            "yt-dlp",
                            isPlaylist ? "--yes-playlist" : "--no-playlist",
                            "-x", "--audio-format", "mp3",
                            url,
                            "-o", currentOutputPath
                    );
                    pb.directory(new File("."));
                    pb.redirectErrorStream(true);
                    currentProcess = pb.start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()));
                    String line;
                    Pattern progressPattern = Pattern.compile("\\[download\\]\\s+(\\d+\\.\\d+)%");
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                        Matcher matcher = progressPattern.matcher(line);
                        if (matcher.find()) {
                            float progress = Float.parseFloat(matcher.group(1));
                            int progressValue = (int) progress;
                            SwingUtilities.invokeLater(() -> {
                                progressBar.setValue(progressValue);
                                progressBar.setString(progressValue + "%");
                            });
                        }
                    }
                    int exitCode = currentProcess.waitFor();
                    SwingUtilities.invokeLater(() -> {
                        if (exitCode == 0) {
                            statusLabel.setText("Status: MP3" + (isPlaylist ? " playlist" : "") + " downloaded to Downloads/Music!");
                            progressBar.setValue(100);
                            progressBar.setString("100%");
                        } else {
                            statusLabel.setText("Status: MP3 download failed. Check console.");
                            progressBar.setValue(0);
                            progressBar.setString("0%");
                        }
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Status: Error: " + ex.getMessage());
                        progressBar.setValue(0);
                        progressBar.setString("0%");
                    });
                }
            });
            downloadThread.start();
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            YTDownloader app = new YTDownloader();
            app.setVisible(true);
        });
    }
}