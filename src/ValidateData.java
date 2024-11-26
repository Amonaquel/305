import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class ValidateData {
    
        public String getValidDate(DataInputStream in, DataOutputStream out, String prompt) throws IOException {
        String date = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);

        while (true) {
            out.writeUTF(prompt);
            date = in.readUTF();

            // Validate the date
            try {
                Date parsedDate = (Date) sdf.parse(date);
                Date currentDate = new Date(0);
                if (parsedDate.before(currentDate)) {
                    out.writeUTF("Date cannot be in the past. Please enter a valid date.");
                    continue;
                }
                break; // Valid date entered
            } catch (ParseException e) {
                out.writeUTF("Invalid date format. Please enter in the format YYYY-MM-DD.");
            }
        }

        return date;
    }

    public String getValidDate(DataInputStream in, DataOutputStream out, String prompt, String startDate) throws IOException {
        String date = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);
        Date startParsedDate = null;

        try {
            startParsedDate = (Date) sdf.parse(startDate);
        } catch (ParseException e) {
            out.writeUTF("Invalid start date format.");
            return null;
        }

        while (true) {
            out.writeUTF(prompt);
            date = in.readUTF();

            // Validate the date
            try {
                Date parsedDate = (Date) sdf.parse(date);
                if (parsedDate.before(startParsedDate)) {
                    out.writeUTF("End date cannot be before start date. Please enter a valid end date.");
                    continue;
                }
                break; // Valid end date entered
            } catch (ParseException e) {
                out.writeUTF("Invalid date format. Please enter in the format YYYY-MM-DD.");
            }
        }

        return date;
    }
}
