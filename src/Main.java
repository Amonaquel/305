import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class Main {
    private static Socket socket;
    private static ObjectOutputStream output;
    private static ObjectInputStream input;

    public static void main(String[] args) {
        try {
            socket = new Socket("localhost", 8000); // Connect to server
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());

            SwingUtilities.invokeLater(() -> new LoginFrame()); // Start GUI
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class LoginFrame extends JFrame {
        public LoginFrame() {
            setTitle("Login");
            setSize(400, 350);  // Adjusted size for a more compact form
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationRelativeTo(null);

            // Use GridBagLayout for better control over positioning
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 10, 5, 10);  // Add spacing between components
            gbc.anchor = GridBagConstraints.CENTER;  // Center components

            // Create components
            JLabel welcomeLabel = new JLabel("Welcome To Car Renting App");
            welcomeLabel.setFont(new Font("Arial", Font.BOLD, 18));
            welcomeLabel.setForeground(Color.BLACK);


            JTextField usernameField = new JTextField(20);
            setHint(usernameField, "Username");


            JPasswordField passwordField = new JPasswordField(20);
            setHint(passwordField, "Password");

            JButton loginButton = new JButton("Login");
            loginButton.setBackground(Color.BLACK);  // Button color
            loginButton.setForeground(Color.WHITE);
            loginButton.setFont(new Font("Arial", Font.BOLD, 16));

            JLabel signUpLabel = new JLabel("Don't have an account? Sign up here!");
            signUpLabel.setForeground(Color.BLUE);
            signUpLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // Layout the components
            gbc.gridx = 0;
            gbc.gridy = 0;
            add(welcomeLabel, gbc);

//            gbc.gridy = 1;
//            add(usernameLabel, gbc);

            gbc.gridy = 2;
            add(usernameField, gbc);

//            gbc.gridy = 3;
//            add(passwordLabel, gbc);

            gbc.gridy = 3;
            add(passwordField, gbc);

            gbc.gridy = 5;
            add(loginButton, gbc);

            gbc.gridy = 6;
            add(signUpLabel, gbc);

            // Actions
            loginButton.addActionListener(e -> {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());

                try {
                    output.writeInt(1); // LOGIN option
                    output.writeUTF(username);
                    output.writeUTF(password);
                    output.flush();

                    String response = input.readUTF();
                    if (response.equals("SUCCESS")) {
                        new HomeFrame(username);
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(this, "Login failed! Check your credentials.");
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });

            signUpLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    new SignUpFrame();
                    dispose();
                }
            });

            setVisible(true);
        }

        // Helper method to set the hint text for JTextField and JPasswordField
        private void setHint(JTextField field, String hint) {
            field.setText(hint);
            field.setForeground(Color.GRAY);

            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (field.getText().equals(hint)) {
                        field.setText("");
                        field.setForeground(Color.BLACK);
                    }
                }

                @Override
                public void focusLost(FocusEvent e) {
                    if (field.getText().isEmpty()) {
                        field.setForeground(Color.GRAY);
                        field.setText(hint);
                    }
                }
            });
        }

        private void setHint(JPasswordField field, String hint) {
            field.setEchoChar((char) 0);  // Disable echo (show password characters)
            field.setText(hint);
            field.setForeground(Color.GRAY);

            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (new String(field.getPassword()).equals(hint)) {
                        field.setText("");
                        field.setForeground(Color.BLACK);
                        field.setEchoChar('*'); // Enable echo for password
                    }
                }

                @Override
                public void focusLost(FocusEvent e) {
                    if (new String(field.getPassword()).isEmpty()) {
                        field.setForeground(Color.GRAY);
                        field.setText(hint);
                        field.setEchoChar((char) 0); // Disable echo (show password characters)
                    }
                }
            });
        }
    }

    static class SignUpFrame extends JFrame {
        public SignUpFrame() {
            setTitle("Sign Up");
            setSize(400, 300);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationRelativeTo(null);

            // Layout with background color
            JPanel panel = new JPanel();
            panel.setLayout(new GridLayout(5, 1, 10, 10));
            panel.setBackground(Color.PINK);

            JLabel usernameLabel = new JLabel("Username:");
            JTextField usernameField = new JTextField();

            JLabel passwordLabel = new JLabel("Password:");
            JPasswordField passwordField = new JPasswordField();

            JButton signUpButton = new JButton("Sign Up");
            signUpButton.setBackground(Color.YELLOW);  // Button color
            signUpButton.setFont(new Font("Arial", Font.BOLD, 16)); // Button font
            signUpButton.setPreferredSize(new Dimension(200, 50));

            // Add to panel
            panel.add(usernameLabel);
            panel.add(usernameField);
            panel.add(passwordLabel);
            panel.add(passwordField);
            panel.add(signUpButton);

            add(panel);

            // Actions
            signUpButton.addActionListener(e -> {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());

                try {
                    output.writeInt(2); // SIGNUP option
                    output.writeUTF(username);
                    output.writeUTF(password);
                    output.flush();

                    String response = input.readUTF();
                    if (response.equals("SUCCESS")) {
                        JOptionPane.showMessageDialog(this, "Sign-up successful! Please login.");
                        new LoginFrame();
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(this, "Sign-up failed! Try again.");
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });

            setVisible(true);
        }
    }

    static class HomeFrame extends JFrame {
        public HomeFrame(String username) {
            setTitle("Home - Welcome " + username);
            setSize(600, 400);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationRelativeTo(null);

            // Layout with color
            JPanel panel = new JPanel();
            panel.setLayout(new FlowLayout(FlowLayout.CENTER));
            panel.setBackground(Color.LIGHT_GRAY);

            JButton myRentalsButton = new JButton("My Rentals");
            JButton rentCarButton = new JButton("Rent a Car");

            myRentalsButton.setPreferredSize(new Dimension(200, 50));
            rentCarButton.setPreferredSize(new Dimension(200, 50));

            // Customize button appearance
            myRentalsButton.setBackground(Color.GREEN);
            rentCarButton.setBackground(Color.GREEN);

            panel.add(myRentalsButton);
            panel.add(rentCarButton);

            add(panel);

            // Actions
            myRentalsButton.addActionListener(e -> new MyRentalsFrame(username));
            rentCarButton.addActionListener(e -> new RentCarFrame(username));

            setVisible(true);
        }
    }

    static class RentCarFrame extends JFrame {
        public RentCarFrame(String username) {
            setTitle("Rent a Car");
            setSize(600, 400);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setLocationRelativeTo(null);

            // Layout with color
            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());
            panel.setBackground(Color.WHITE);

            DefaultTableModel tableModel = new DefaultTableModel(new String[]{"Car Name", "License Plate"}, 0);
            JTable carTable = new JTable(tableModel);
            JScrollPane scrollPane = new JScrollPane(carTable);

            JPanel datePanel = new JPanel(new GridLayout(2, 2));
            JLabel startDateLabel = new JLabel("Start Date:");
            JLabel endDateLabel = new JLabel("End Date:");
            JTextField startDateField = new JTextField();
            JTextField endDateField = new JTextField();

            JButton rentButton = new JButton("Confirm Rental");
            rentButton.setBackground(Color.YELLOW);  // Button color
            rentButton.setPreferredSize(new Dimension(200, 50));  // Button size

            datePanel.add(startDateLabel);
            datePanel.add(startDateField);
            datePanel.add(endDateLabel);
            datePanel.add(endDateField);

            panel.add(scrollPane, BorderLayout.CENTER);
            panel.add(datePanel, BorderLayout.SOUTH);
            panel.add(rentButton, BorderLayout.NORTH);

            add(panel);

            // Fetch available cars
            try {
                output.writeInt(3); // GET_CARS option
                output.flush();

                ArrayList<String> cars = (ArrayList<String>) input.readObject();
                for (String car : cars) {
                    tableModel.addRow(new Object[]{car.split(" \\(")[0], car.split("\\(")[1].replace(")", "")});
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

            // Actions
            rentButton.addActionListener(e -> {
                int selectedRow = carTable.getSelectedRow();
                if (selectedRow == -1) {
                    JOptionPane.showMessageDialog(this, "Please select a car.");
                    return;
                }

                String carName = tableModel.getValueAt(selectedRow, 0).toString();
                String licensePlate = tableModel.getValueAt(selectedRow, 1).toString();
                String startDate = startDateField.getText();
                String endDate = endDateField.getText();

                // Validate date format
                if (!isValidDate(startDate) || !isValidDate(endDate)) {
                    JOptionPane.showMessageDialog(this, "Invalid date format. Please use yyyy-MM-dd.");
                    return;
                }

                // Ensure startDate is before endDate
                if (startDate.compareTo(endDate) > 0) {
                    JOptionPane.showMessageDialog(this, "Start date cannot be after end date.");
                    return;
                }

                try {
                    output.writeInt(4); // RENT_CAR option
                    output.writeUTF(username);
                    output.writeUTF(licensePlate);
                    output.writeUTF(startDate);
                    output.writeUTF(endDate);
                    output.flush();

                    String response = input.readUTF();
                    if (response.equals("SUCCESS")) {
                        JOptionPane.showMessageDialog(this, "Car rented successfully!");
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(this, "Car rental failed!");
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });

            setVisible(true);
        }

        // Helper method to validate date format
        private boolean isValidDate(String date) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setLenient(false);  // Ensure strict parsing (no automatic adjustments)
            try {
                sdf.parse(date);  // Try parsing the date string
                return true;
            } catch (ParseException e) {
                return false;
            }
        }
    }

    static class MyRentalsFrame extends JFrame {
        public MyRentalsFrame(String username) {
            setTitle("My Rentals");
            setSize(600, 400);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setLocationRelativeTo(null);

            // Layout with color
            DefaultTableModel tableModel = new DefaultTableModel(new String[]{"Car Name", "Start Date", "End Date"}, 0);
            JTable rentalTable = new JTable(tableModel);
            JScrollPane scrollPane = new JScrollPane(rentalTable);

            add(scrollPane);

            // Fetch rentals
            try {
                output.writeInt(5); // MY_RENTALS option
                output.writeUTF(username);
                output.flush();

                ArrayList<String> rentals = (ArrayList<String>) input.readObject();
                for (String rental : rentals) {
                    String[] parts = rental.split("\\s+\\(");
                    String carName = parts[0];
                    String dates = parts[1].replace(")", "");
                    String[] dateParts = dates.split("\\s+-\\s+");

                    tableModel.addRow(new Object[]{carName, dateParts[0], dateParts[1]});
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

            setVisible(true);
        }
    }
}
