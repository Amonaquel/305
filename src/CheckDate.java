import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CheckDate {
    public String getValidDate(DataInputStream in, DataOutputStream out, String prompt) throws IOException {
        String date;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);

        while (true) {
            out.writeUTF(prompt);
            date = in.readUTF();

            try {
                java.util.Date parsedDate = sdf.parse(date);
                java.util.Date currentDate = new java.util.Date();
                if (parsedDate.before(currentDate)) {
                    out.writeUTF("Date cannot be in the past. Please enter a valid date.");
                    continue;
                }
                return date;
            } catch (ParseException e) {
                out.writeUTF("Invalid date format. Please enter in the format YYYY-MM-DD.");
            }
        }
    }

    public String getValidDate(DataInputStream in, DataOutputStream out, String prompt, String startDate) throws IOException {
        String date;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);
        java.util.Date startParsedDate;

        try {
            startParsedDate = sdf.parse(startDate);
        } catch (ParseException e) {
            out.writeUTF("Invalid start date format.");
            return null;
        }

        while (true) {
            out.writeUTF(prompt);
            date = in.readUTF();

            try {
                Date parsedDate = sdf.parse(date);
                if (parsedDate.before(startParsedDate)) {
                    out.writeUTF("End date cannot be before start date. Please enter a valid end date.");
                    continue;
                }
                return date;
            } catch (ParseException e) {
                out.writeUTF("Invalid date format. Please enter in the format YYYY-MM-DD.");
            }
        }
    }
}
