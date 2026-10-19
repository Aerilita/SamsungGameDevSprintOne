import java.io.PrintStream;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

record Problem( // для задачек
		String text, int answer) {
}

interface Entity { // пойдем еще дальше

	int getX();

	int getY();

}

class Person implements Entity {// мы с детьми на Java делаем персонажей через классы, очень не хочется все в
	// main скидывать
	private int lives = 3;
	private static final String visual = "\uD83D\uDC64";
	private int x;
	private int y;

	public Person(int boardSize) {// раз мы всегда на краю, то не буду делать дефолтный с x,y
		this.x = 1 + ThreadLocalRandom.current().nextInt(boardSize);
		this.y = boardSize;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	@Override
	public String toString() {
		return visual;
	}

	public int getLives() {
		return lives;
	}

	public void setPosition(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public void downLife() {
		lives--;
	}

	/*
	 * public void upLife() { lives++; }
	 */
}

class Monster implements Entity {// из тех же соображений

	public static final int MOB_TYPE_SMALL = 0, MOB_TYPE_BIG = 1; // во все тяжкие
	// public static final String VIS_SMALL_TEXT = "Мм", VIS_BIG_TEXT = "Мб";
	// мне лень делать текстовый переключатель
	private static final String VIS_SMALL = "\uD83D\uDC31", VIS_BIG = "\uD83E\uDDDF";

	private final int type;
	private final String visual;
	private final int x;
	private final int y;

	public Monster(int type, int x, int y) {
		this.type = type;
		this.x = x;
		this.y = y;
		visual = type == MOB_TYPE_SMALL ? VIS_SMALL : VIS_BIG;
	}

	public int getType() {
		return type;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public String toString() {
		return visual;
	}

}

class Castle implements Entity {

	private static final String visual = "\uD83C\uDFF0";
	private final int x;
	private final int y;

	public Castle(int boardSize) {
		this.x = 1 + ThreadLocalRandom.current().nextInt(boardSize);
		this.y = 1;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	@Override
	public String toString() {
		return visual;
	}

}

class GameField {
	private static final int CELL_WIDTH = 4;
	private Entity[][] board;
	private boolean gfDebug = false;
	private int boardSize;

	GameField(Entity[][] board) {
		this.boardSize = board.length;
		this.board = board;
	}

	Entity get(int x, int y) {
		return board[x - 1][y - 1];
	}

	@Override
	public String toString() {
		String temp = "";
		String fill = "";
		String log = "";
		for (int i = 0; i < boardSize; i++) { // i = row = y
			for (int j = 0; j < boardSize; j++) { // верхние границы ячеек
				temp += "+ —— ";
			}
			temp += "+\n";
			for (int j = 0; j < boardSize; j++) {
				Entity e = board[j][i];
				if (e == null) {
					fill = String.format("%" + CELL_WIDTH + "s", "");
				} else {
					fill = middlePad(e.toString(), CELL_WIDTH);
					log += e.toString() + " x = " + e.getX() + ", y = " + e.getY() + "\n";
				}
				temp += "|" + fill;
			}
			temp += "|\n"; // правая граница
		}
		for (int j = 0; j < boardSize; j++) { // нижняя граница
			temp += "+ —— ";
		}
		temp += "+\n";

		if (gfDebug)
			temp += "\n" + log;
		return temp;
	}

	private String middlePad(String str, int targetLen) {
		int len = str.length();
		// внутри Eclipse работает нормально, но если заставят переделать, то через
		// int len = str.codePointCount(0, str.length())*2;
		if (len > targetLen) {
			return str;
		} else {
			int pl = (targetLen - len) / 2; // pad left
			int pr = targetLen - len - pl; // pad right
			String result = String.format("%" + pl + "s", "") + str + String.format("%" + pr + "s", "");
			return result;
		}
	}

	boolean isMoveCorrect(Person e, int x, int y) {
		if (x < 1 || x > boardSize || y < 1 || y > boardSize)
			return false;
		return e.getX() == x && Math.abs(e.getY() - y) == 1 || e.getY() == y && Math.abs(e.getX() - x) == 1;
	}

	void move(Person e, int x, int y) {
		board[e.getX() - 1][e.getY() - 1] = null;
		board[x - 1][y - 1] = e; // нет проверки на то, что тут стояло, но надо же когда-то остановиться
		e.setPosition(x, y);
	}

}

class Game {
	public static final int BOARD_SIZE = 5;
	private int moveCount = 0; // ходы как в играх, а не физический шаг
	private GameField gameField;
	private Person player; // главный герой
	private boolean gameOver = false;
	public static final int BIG_SHARE = 2; // больших монстров - 2/5
	public static final int SMALL_SHARE = 3;
	// private int bigMonsterCount = 6; // столько было на рисунке в ТЗ
	private int bigMonsterCount = (BOARD_SIZE * BOARD_SIZE - BOARD_SIZE - 5) * BIG_SHARE / (SMALL_SHARE + BIG_SHARE);
	// private int smallMonsterCount = 9; // столько было на рисунке в ТЗ
	private int smallMonsterCount = (BOARD_SIZE * BOARD_SIZE - BOARD_SIZE - 5) * SMALL_SHARE
			/ (SMALL_SHARE + BIG_SHARE);

	private int difficulty;
	private PrintStream out;
	private Scanner in;

	private boolean gameDebug = false;
	private static final int MAX_LINEAR = 200;
	private static final int MAX_MULTIPLE = 13;

	Game(Scanner in, PrintStream out) {
		this.in = in;
		this.out = out;
	}

	public void start() {
		out.println("Ты готов начать играть в игру? (Напиши: ДА или НЕТ):");
		String answer = in.nextLine();

		if (answer.equals("ДА") || answer.equals("1")) {
			out.println("Начинаем играть");
		} else {
			out.println("Почему ты не захотел со мной играть(");
			out.println("Приходи ещё!");
			return;
		}
		out.println("Выбери сложность игры(от 1 до 5):");
		difficulty = in.nextInt();
		if (difficulty < 1 || difficulty > 5) {
			out.println("Сложность вне диапазона 1–5, ставлю 3. В следующий раз вводи аккуратнее!");
			difficulty = 3;
		}
		player = new Person(BOARD_SIZE);
		Castle castle = new Castle(BOARD_SIZE);
		Entity[][] raw = generateEntities(player, castle); // полумера, в идеале привести к MVC
		gameField = new GameField(raw);

		while (!gameOver) {

			out.print(gameField);

			out.println(
					"Укажите, куда будет ходить персонаж\n(ход возможен только по вертикали и горизонтали на одну клетку)");
			out.println("Текущие координаты персонажа - (x: " + player.getX() + ", y: " + player.getY() + "))");
			out.println("Введите новые координаты через пробел");
			int newX = in.nextInt();
			int newY = in.nextInt();
			in.nextLine();
			if (gameField.isMoveCorrect(player, newX, newY)) {
				moveCount++;
				boolean success = false;
				Entity e = gameField.get(newX, newY);
				if (e != null) {
					if (e instanceof Castle) {// мы ведь в Java 16+, да?
						out.println("Вы прошли игру");
						gameOver = true;
						success = true;

					} else if (e instanceof Monster m) {
						String mSizeStr = "";
						switch (m.getType()) {// нуавдруг у нас будет больше типов
						case Monster.MOB_TYPE_BIG:
							mSizeStr = "большого ";
							break;
						case Monster.MOB_TYPE_SMALL:
							mSizeStr = "маленького ";
							break;
						default:
							mSizeStr = "неизвестного "; // если накосячим с добавлением
						}
						out.println("Решите задачу, чтобы победить " + mSizeStr + "монстра.");
						if (m.getType() == Monster.MOB_TYPE_SMALL)
							success = mathBattle(1); // если моб маленький, то и задача простая 
						else
							success = mathBattle(difficulty);

					}
				} else {
					success = true;
				}
				if (success) {
					gameField.move(player, newX, newY);
					out.println("Ход корректный; Новые координаты: " + player.getX() + ", " + player.getY());
				}
				out.println("Ход номер: " + moveCount);

			} else {
				out.println("Некорректный ход");
			}
		}
	}

	private boolean mathBattle(int difficulty) {
		boolean success = false;
		Problem p = generateMathProblem(difficulty);
		out.println("Реши пример: " + p.text() + " = ?");
		int ans = in.nextInt();
		if (ans == p.answer()) {
			out.println("Верно! Вы победили монстра.");
			success = true;
		} else {
			player.downLife();
			if (player.getLives() > 0) {
				out.println("Вы проиграли эту битву. Жизней осталось: " + player.getLives());

			} else {
				gameOver = true;
				out.println("Игра закончена. Вы проиграли.");
			}
		}
		return success;
	}

	/* почему Greatest Common Divisor только в BigInteger?! */
	private int gcd(int a, int b) {
		return b == 0 ? a : gcd(b, a % b);
		// можно я не буду выводить с нуля из алгоритма Евклида, этот код гуглится
	}

	private int lcm(int a, int b) {
		return a / gcd(a, b) * b;
	}

	private Problem generateMathProblem(int difficulty) {
		ThreadLocalRandom r = ThreadLocalRandom.current(); // безопасный рандом
		switch (difficulty) {
		case 1: {// сложение
			int a = r.nextInt(MAX_LINEAR);
			int b = r.nextInt(MAX_LINEAR);
			return new Problem(a + " + " + b, a + b);
		}
		case 2: { // вычитание (a >= b, чтобы ответ неотрицательный)
			int a = r.nextInt(MAX_LINEAR);
			int b = r.nextInt(a + 1);
			return new Problem(a + " - " + b, a - b);
		}
		case 3: { // умножение
			int a = r.nextInt(2, MAX_MULTIPLE);
			int b = r.nextInt(2, MAX_MULTIPLE);
			return new Problem(a + " * " + b, a * b);
		}
		case 4: { // деление нацело
			int b = r.nextInt(2, MAX_MULTIPLE);
			int ans = r.nextInt(2, MAX_MULTIPLE);
			int a = b * ans; // чтобы делилось без остатка
			return new Problem(a + " / " + b, ans);
		}
		case 5:
		default: { // НОК
			int a = r.nextInt(2, MAX_MULTIPLE);
			int b;
			do {
				b = r.nextInt(2, MAX_MULTIPLE);
			} while (b == a); // чтоб не совпало с a
			return new Problem("Наименьшее общее кратное (" + a + ", " + b + ")", lcm(a, b));
		}
		}
	}

	private Entity[][] generateEntities(Entity player, Entity castle) {
		Entity[][] board = new Entity[BOARD_SIZE][BOARD_SIZE];
		board[player.getX() - 1][player.getY() - 1] = player;
		board[castle.getX() - 1][castle.getY() - 1] = castle;
		generateMonsters(board, bigMonsterCount, Monster.MOB_TYPE_BIG);
		generateMonsters(board, smallMonsterCount, Monster.MOB_TYPE_SMALL);

		return board;
	}

	private void generateMonsters(Entity[][] board, int monsterCount, int monsterType) {
		int count = monsterCount;
		int attempts = 0;
		int maxAttempts = 1000; // чтобы избежать бесконечного цикла (маловероятно, но...)
		while (count > 0 && attempts < maxAttempts) {
			attempts++;
			int x = ThreadLocalRandom.current().nextInt(BOARD_SIZE);
			int y = ThreadLocalRandom.current().nextInt(BOARD_SIZE);
			if (gameDebug) {
				out.println("x = " + x + " y = " + y);
				out.println("board[x][y] = " + board[x][y]);
			}
			if (board[x][y] == null) {
				board[x][y] = new Monster(monsterType, x, y);
				if (gameDebug) {
					out.println("generating monster at x = " + x + " y = " + y);
					out.println("board[x][y] = " + board[x][y]);
				}
				count--;
			}
		}
		if (count > 0) {
			out.println("Предупреждение: не удалось разместить " + count + " монстров"); // (маловероятно, но...)
		}

	}

}

public class SprintOne {

	public static void main(String[] args) {
		Scanner in = new Scanner(System.in);
		PrintStream out = System.out;
		Game game = new Game(in, out);
		game.start();
		in.close(); // закрываем здесь, а не в конструкторе

	}

}
