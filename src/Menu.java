import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class Menu {
    public void execute(DataInputStream in, DataOutputStream out, Connection conn, int userId) throws IOException {
        boolean check = false;

        while (!check) {
            out.writeUTF("Enter number: 1 to see car list, 2 to see your rental cars, 3 to Exit: ");

            try {
                int option = in.readInt();
                if (option == 1) {
                    CarList carList = new CarList();
                    carList.showCarListAndHandleSelection(in, out, conn, userId);
                    check = true;
                } else if (option == 2) {
                    Rental rental = new Rental();
                    rental.execute(in, out, conn, userId);
                    check = true;
                }else if (option == 3) {
                    out.writeUTF("Thanks for using our car rent app :) ");
                }else {
                    out.writeUTF("Invalid option selected. Please enter 1 for car list or 2 for rental cars or 3 for exit.");
                }
            } catch (IOException | NumberFormatException e) {
                out.writeUTF("Invalid input. Please enter a valid number (1 or 2 or 3).");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
