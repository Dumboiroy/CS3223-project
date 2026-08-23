package simpledb.parse;
import java.util.Scanner;

// Will successfully read in lines of text denoting an
// SQL expression of the form "id = c" or "c = id".

public class LexerTest {
	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		System.out.println("Enter SQL expressions (id = value or value = id). Type Ctrl+D to exit:");
		while (sc.hasNext()) {
			try {
				String s = sc.nextLine();
				Lexer lex = new Lexer(s);
				String x; String y; String opr;
				
				// Parse first operand
				if (lex.matchId()) {
					x = lex.eatId();
			    } else if (lex.matchIntConstant()) {
			        x = String.valueOf(lex.eatIntConstant());
			    } else if (lex.matchStringConstant()) {
			        x = "'" + lex.eatStringConstant() + "'";
			    } else {
			        throw new BadSyntaxException();
			    }
				
				// Parse operator
				opr = lex.eatOpr();
				
				// Parse right operand
				if (lex.matchId()) {
					y = lex.eatId();
			    } else if (lex.matchIntConstant()) {
			        y = String.valueOf(lex.eatIntConstant());
			    } else if (lex.matchStringConstant()) {
			        y = "'" + lex.eatStringConstant() + "'";
			    } else {
			        throw new BadSyntaxException();
			    }
				
				// Print expression
				System.out.println(x + " " + opr + " " + y);
			} 
			catch (BadSyntaxException ex) {
	            System.out.println("Invalid Expression: " + ex);
			}
		}
		sc.close();
	}
}
