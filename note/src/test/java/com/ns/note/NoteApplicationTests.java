package com.ns.note;

import com.ns.note.note.repository.NoteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest()
class NoteApplicationTests {
    @MockitoBean
	private NoteRepository noteRepository;

	@Test
	void contextLoads() {
	}

}
