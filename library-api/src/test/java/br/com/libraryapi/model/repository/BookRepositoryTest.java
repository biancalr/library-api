package br.com.libraryapi.model.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import br.com.libraryapi.model.entity.Book;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@DataJpaTest
public class BookRepositoryTest {

	@Autowired
	private TestEntityManager entityManager;
	
	@Autowired
	private BookRepository repository;
	
	@Test
	@DisplayName("Deve retornar verdadeiro quando existir um livro na base com o ISBN informado")
	public void returnTrueWhenIsnExists() {
		//cenario
		String isbn = "123";
		Book book = createNewBook(isbn);
		entityManager.persist(book);
		
		//execucao
		boolean exists = repository.existsByIsbn(isbn);
		
		//verificacao
		assertThat(exists).isTrue();
	}

	public static Book createNewBook(String isbn) {
		return Book.builder().title("As aventuras").author("Fulano").isbn(isbn).build();
	}
	
	@Test
	@DisplayName("Deve retornar falso quando nao existir um livro na base com o ISBN informado")
	public void returnTrueWhenIsnDoesnExists() {
		//cenario
		String isbn = "123";
		
		//execucao
		boolean exists = repository.existsByIsbn(isbn);
		
		//verificacao
		assertThat(exists).isFalse();
	}
	
	@Test
	@DisplayName("Deve obter um livro pro id")
	public void findByIdTest() {
		
		// cenario
		Book book = createNewBook("123");
		entityManager.persist(book);
		
		// execucao
		Optional<Book> foundBook = repository.findById(book.getId());
		
		// verificacao
		assertThat(foundBook.isPresent()).isTrue();
	}
	
	@Test
	@DisplayName("Deve salvar um livro.")
	public void saveBookTest() {
		
		// cenario
		Book book = createNewBook("123");
		
		// execucao
		Book savedBook = repository.save(book);
		
		// verificacao
		assertThat(savedBook.getId()).isNotNull();
	}
	
	@Test
	@DisplayName("Deve deletar um livro")
	public void deleteBookTest() {
		
		// cenario
		Book book = createNewBook("123");
		entityManager.persist(book);
		Book foundBook = entityManager.find(Book.class, book.getId());
		
		// execucao
		repository.delete(foundBook);
		
		Book deletedBook = entityManager.find(Book.class, book.getId());
		
		// verificacao
		assertThat(deletedBook).isNull();
		
	}
	
	
	
	
	
	
}
