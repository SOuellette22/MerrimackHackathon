import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

public class Main {

    public static void main(String[] args) {
        Turtle bob = new Turtle();

        for(int i=0;i<360;i++)

        {

            bob.forward(i*1.25);

            bob.left(90.25);

        }
    }

    public void deserialize(JSONType type) {
        if (!(type instanceof JSONObject)) {
            throw new IllegalArgumentException("Expected a JSONObject");
        }


    }
}
