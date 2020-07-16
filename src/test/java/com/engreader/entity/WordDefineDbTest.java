package com.engreader.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.eclipse.jetty.io.RuntimeIOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.alibaba.fastjson.JSON;
import com.engreader.StanfordNLPStemmer;
import com.engreader.db.H2DB;
import com.engreader.db.WordDefineDB;

class WordDefineDbTest {
	H2DB h2db;
	WordDefineDB db;

	@BeforeEach
	void setup() throws ClassNotFoundException, SQLException {
		h2db = H2DB.connect();
		db = new WordDefineDB(h2db);
	}

	@AfterEach
	void tearDown() throws SQLException {
		h2db.close();
	}

	@Test
	void resetTable() {
		db.removeAll();
		db.dropTable();
		db.createTable();
		db.createIndex();
	}

	@Test
	void testWordDefineStore() {
	}

	@Test
	void testRemoveAll() {
	}

	@Test
	void testGetAll() {
		List<WordDefine> list = db.getAll();
		assertTrue(list.size() > 1000);
	}

	@Test
	void testInsertUpdate() {
		// @formatter:off
		String js = "{  \"word\": \"book\",\r\n" + "  \"accentUs\": \"/bʊk/\",\r\n"
				+ "  \"meanZh\": \"n. 书，书籍；  v. 预订\",\r\n"
				+ "  \"meanEn\": \"a set of printed pages that are fastened inside a cover so that you can turn them and read them\"}";
		// @formatter:on

		WordDefine define = JSON.parseObject(js, WordDefine.class);

		define.setId(100);
		db.insert(define);

		WordDefine inserted = db.get(100);
		inserted.meanZh = "inserted" + inserted.meanZh;

		db.update(inserted);

		WordDefine updated = db.get(100);
		assertEquals("inserted" + define.meanZh, updated.getMeanZh());

		System.out.println(define);
//		WordDefine wd = new WordDefine(1, null, null, null, null, null, null, null, null, 0, null)
//		
//		db.insert(null);
	}

	@Test

	void importDataFromCsv() throws IOException {
		db.removeAll();

		String[] filenames = new String[] { "CSVFile/good_dictionary_a_b.csv", "CSVFile/good_dictionary_c.csv",
				"CSVFile/good_dictionary_d_f.csv", "CSVFile/good_dictionary_g_k.csv", "CSVFile/good_dictionary_l_o.csv",
				"CSVFile/good_dictionary_p_r.csv", "CSVFile/good_dictionary_s.csv", "CSVFile/good_dictionary_t_z.csv" };

		Map<String, WordDefine> map = new HashMap<>(100000);
		for (String filename : filenames) {
			List<WordDefine> wd = readfileAndInsert(filename);
			for (WordDefine wordDefine : wd) {
				map.put(wordDefine.word, wordDefine);
			}
			System.out.println("insert from " + filename + " " + wd.size());
		}

		{// Coca20000
			List<WordDefine> needUpdated = new ArrayList<>();
			List<WordDefine> others = new ArrayList<>();

			List<CocaWord> cocas = readCocaWords("CSVFile/COCA20000.csv");

			for (int i = cocas.size() - 1; i >= 0; i--) {
				CocaWord cocaWord = cocas.get(i);
				if (cocaWord.lemma.equals("you")) {
					System.out.println(cocaWord);
				}
				if (map.containsKey(cocaWord.getLemma())) {
					WordDefine wordDefine = map.get(cocaWord.getLemma());
//					wordDefine.meanBriefZh = cocaWord.getMeaningTip();
					wordDefine.pos = cocaWord.getPos();
					wordDefine.cocaLevel = cocaWord.getLevel();
					wordDefine.cocaRankFrequency = cocaWord.getRankFrequency();
					wordDefine.cocaRawFrequency = cocaWord.getRawFrequency();
					wordDefine.cocaDispersion = cocaWord.getDispersion();
					needUpdated.add(wordDefine);
				} else {
					WordDefine wordDefine = new WordDefine();

					wordDefine.id = 2000000 + cocaWord.getRankFrequency();
					wordDefine.word = cocaWord.getLemma();
//				String tense;
//				String accentEn;
//				String accentUs;
					wordDefine.meanZh = cocaWord.getMeaning();
					wordDefine.meanBriefZh = cocaWord.getMeaningTip();
//				String meanEn;
					wordDefine.pos = cocaWord.getPos();
//				int freq;
					wordDefine.cocaRankFrequency = cocaWord.getRankFrequency();
					wordDefine.cocaRawFrequency = cocaWord.getRawFrequency();
					wordDefine.cocaDispersion = cocaWord.getDispersion();
//				Timestamp updated;

					others.add(wordDefine);

					map.put(wordDefine.word, wordDefine);
				}
			}

			db.update(needUpdated);
			db.insert(others);
			System.out.println("update from COCA20000 " + needUpdated.size());
			System.out.println("insert from COCA20000 " + others.size());
		}

		{// Coca60000
			List<WordDefine> needUpdated = new ArrayList<>();
			List<WordDefine> others = new ArrayList<>();

			List<Coca60000> cocas = readCoca60000("CSVFile/COCA60000.csv");
			for (Coca60000 c : cocas) {
				String word = c.getWord();
				if (word.startsWith("(") && word.endsWith(")")) {
					word = word.substring(1, word.length() - 1);
				}
				if (map.containsKey(word)) {
					WordDefine wordDefine = map.get(word);
//					wordDefine.meanBriefZh = c.getMeaningTip();
//					wordDefine.pos = c.getPos();
//					wordDefine.cocaLevel = c.getLevel();
//					wordDefine.cocaRankFrequency = c.getRankFrequency();
//					wordDefine.cocaRawFrequency = c.getRawFrequency();
//					wordDefine.cocaDispersion = c.getDispersion();
					wordDefine.coca60kRank = c.getCoca60kRank();
//					wordDefine.cocaPos =c.getCocaPos();
					wordDefine.cocaTotal = c.getCocaTotal();
					wordDefine.cocaSpoken = c.getCocaSpoken();
					wordDefine.cocaFiction = c.getCocaFiction();
					wordDefine.cocaMagazine = c.getCocaMagazine();
					wordDefine.cocaNewspaper = c.getCocaNewspaper();
					wordDefine.cocaAcademic = c.getCocaAcademic();

					needUpdated.add(wordDefine);
				} else {
					WordDefine wordDefine = new WordDefine();

					wordDefine.id = 6000000 + c.getCoca60kRank();
					wordDefine.word = c.getWord();

					wordDefine.coca60kRank = c.getCoca60kRank();
					wordDefine.cocaTotal = c.getCocaTotal();
					wordDefine.cocaSpoken = c.getCocaSpoken();
					wordDefine.cocaFiction = c.getCocaFiction();
					wordDefine.cocaMagazine = c.getCocaMagazine();
					wordDefine.cocaNewspaper = c.getCocaNewspaper();
					wordDefine.cocaAcademic = c.getCocaAcademic();

					others.add(wordDefine);

					map.put(wordDefine.word, wordDefine);
				}
			}

			db.update(needUpdated);
			db.insert(others);
			System.out.println("update from COCA60000 " + needUpdated.size());
			System.out.println("insert from COCA60000 " + others.size());
		}
//		ps.getRecords()
	}

	private List<WordDefine> readfileAndInsert(String filename) throws IOException {
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(filename);

//		"topic_id","word","accent","mean_cn","freq","word_length"

		CSVParser ps = CSVParser.parse(is, Charset.forName("utf-8"), CSVFormat.EXCEL.withHeader());
		List<WordDefine> list = new ArrayList<>();

		for (CSVRecord r : ps) {
			int i = 0;
			int id = Integer.parseInt(r.get(i++));
			String word = r.get(i++);
			String accentUs = r.get(i++);
			String meanZh = r.get(i++);
			String meanBriefZh = filterBrief(meanZh);
			int freq = (int) (Float.parseFloat(r.get(i++)) * 10);
			String pos = null;

			int from = meanZh.indexOf(".");
			if (0 < from && from < 10) {
				pos = meanZh.substring(0, from);
				meanZh = meanZh.substring(from + 1);
			}

			WordDefine wd = new WordDefine(id, word, null, null, accentUs, meanZh, meanBriefZh, null, pos, freq,
					new Timestamp(new Date().getTime()));
			list.add(wd);
		}
		db.insert(list);
		return list;
	}

	@Test
	public void testFilter() {
		assertEquals("灌木",filterBrief("灌木；灌木丛；矮树；  vi. 丛生；灌木般丛生；浓密（或茂密）地生长；生密枝；  vt. 加（金属）衬套于；加套管于；用耙耙平（耕地）；以灌木（或灌木丛）装饰（或覆盖、围绕、围住、支撑、标志、保护等）；  adj. （豆科植物等）如灌木般长得低矮的；粗野的；粗鲁的；粗糙但实用的"));
		assertEquals("离开正路的人",filterBrief("n. 离开正路的人，反常的人；  adj. 偏离正轨的；异常的，畸变的"));
		assertEquals("直的",filterBrief("adj. 直的；正直的；  adv. 直接地；不断地，一直地；立即；直地，笔直地；  n. 直线；直线部分"));	
		
	}
	// 灌木；灌木丛；矮树； vi. 丛生；灌木般丛生；浓密（或茂密）地生长；生密枝； vt.
	// 加（金属）衬套于；加套管于；用耙耙平（耕地）；以灌木（或灌木丛）装饰（或覆盖、围绕、围住、支撑、标志、保护等）； adj.
	// （豆科植物等）如灌木般长得低矮的；粗野的；粗鲁的；粗糙但实用的
	public String filterBrief(String meanZh) {
		int i = 0;
		char[] chars = meanZh.toCharArray();
		for (; i < chars.length && 'a' <= chars[i] && chars[i] <= 'z'; i++) {

		}
		for (; i < chars.length && (chars[i] == ' ' || chars[i] == '.'); i++) {

		}
		int from = i;
		for (; i < chars.length && Character.isLetter(chars[i]); i++) {

		}

		return meanZh.substring(from, i);
	}

	public void importCocaWordFromCsv(String filename) throws IOException {

		try {
			readCocaWords("words20000.csv");

		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeIOException(e);
		}

	}

	private List<CocaWord> readCocaWords(String filename) throws IOException {
		InputStream is = StanfordNLPStemmer.class.getClassLoader().getResourceAsStream(filename);
		CSVParser csvParser = CSVParser.parse(is, Charset.forName("utf-8"), CSVFormat.EXCEL);

		List<CocaWord> cocaWords = new ArrayList<>();

		for (CSVRecord r : csvParser) {
			int rankFrequency = Integer.parseInt(r.get(0));
			String lemma = r.get(1);
			String pos = r.get(2);
			int rawFrequency = Integer.parseInt(r.get(4));
			int dispersion = (int) (Float.parseFloat(r.get(5)) * 100);
			String meaningTip = r.get(9);
			String meaning = r.get(10);

			cocaWords.add(new CocaWord(rankFrequency, lemma, pos, rawFrequency, dispersion, meaningTip, meaning));
		}
		return cocaWords;
	}

	private List<Coca60000> readCoca60000(String filename) throws IOException {
		InputStream is = StanfordNLPStemmer.class.getClassLoader().getResourceAsStream(filename);
		CSVParser csvParser = CSVParser.parse(is, Charset.forName("utf-8"), CSVFormat.EXCEL.withHeader());

		List<Coca60000> cocaWords = new ArrayList<>();

		for (CSVRecord r : csvParser) {
			int coca60kRank = Integer.parseInt(r.get(0));
			String cocaPos = r.get(1);
			String word = r.get(2).toLowerCase();
			int cocaTotal = Integer.parseInt(r.get(3));
			int cocaSpoken = Integer.parseInt(r.get(4));
			int cocaFiction = Integer.parseInt(r.get(5));
			int cocaMagazine = Integer.parseInt(r.get(6));
			int cocaNewspaper = Integer.parseInt(r.get(7));
			int cocaAcademic = Integer.parseInt(r.get(8));

			cocaWords.add(new Coca60000(coca60kRank, cocaPos, word, cocaTotal, cocaSpoken, cocaFiction, cocaMagazine,
					cocaNewspaper, cocaAcademic));
		}
		return cocaWords;
	}

}
