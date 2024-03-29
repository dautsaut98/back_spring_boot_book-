package com.back_spring_boot_book.controlleur;

import com.aol.cyclops.trycatch.Try;
import com.back_spring_boot_book.exceptions.BookNonSupprimeException;
import com.back_spring_boot_book.exceptions.BookNonTrouveException;
import com.back_spring_boot_book.utils.converters.ConvertRequestDTOToEntity;
import com.back_spring_boot_book.utils.converters.ConverterEntityToResponseDTO;
import com.back_spring_boot_book.dtos.requestDto.BookRequestDTO;
import com.back_spring_boot_book.dtos.responseDto.BookResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.back_spring_boot_book.exceptions.BookExisteDejaException;
import com.back_spring_boot_book.exceptions.UtilisateurNonTrouveException;
import com.back_spring_boot_book.model.Book;
import com.back_spring_boot_book.service.serviceImplemente.ServiceBook;
import com.back_spring_boot_book.service.serviceImplemente.ServiceUtilisateur;

import javax.validation.Valid;
import java.text.ParseException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/book")
public class BookController {

    @Autowired
    private ServiceBook serviceBook;
    @Autowired
    private ServiceUtilisateur serviceUtilisateur;
    Logger logger = LoggerFactory.getLogger(BookController.class);

    @PostMapping("/addBook")
    public ResponseEntity<String> addBook(@Valid @RequestBody BookRequestDTO bookRequestDTO) {
        try {
            logger.debug("Appel de addBook avec le livre "+bookRequestDTO.toString());
        	Book book = ConvertRequestDTOToEntity.convertBookDTOToBook(bookRequestDTO);
            book.setId(null);
        	book.setUtilisateur(this.serviceUtilisateur.findUtilisateurById(bookRequestDTO.getIdUser()));
            this.serviceBook.addBook(book);
            logger.debug("retour 200 de addBook");
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (BookExisteDejaException e) {
            logger.debug("retour 409 de addBook le livre "+bookRequestDTO.getNom()+" existe deja en bdd pour l utlisateur "+bookRequestDTO.getIdUser());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(bookRequestDTO.getNom());
        } catch(UtilisateurNonTrouveException e) {
            logger.debug("retour 404 de addBook, l utilisateur "+bookRequestDTO.getIdUser()+" est introuvable en bdd");
        	return ResponseEntity.status(HttpStatus.NOT_FOUND).body(bookRequestDTO.getNom());
        } catch (ParseException e) {
            logger.debug("retour 400 de addBook, la date est non correct");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(bookRequestDTO.getNom());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<BookResponseDTO>> getBookOfUser(@Valid @RequestParam Integer idUser) {
        logger.debug("Appel de getBookOfUser avec l id user "+idUser);
        List<BookResponseDTO> booksDTO = this.serviceBook.findByIdUtilisateur(idUser)
        		.stream()
        		.map(ConverterEntityToResponseDTO::convertBookToBookDTO)
        		.peek(book -> book.setIdUser(idUser))
        		.collect(Collectors.toList());
        if (booksDTO.isEmpty()) {
            logger.debug("retour 404 de getBookOfUser, la liste de livre pour l utilisateur "+idUser+" est introuvable en bdd");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        logger.debug("retour 200 de getBookOfUser avec pour l utiilisateur "+idUser+" avec la liste de livre : "+booksDTO);
        return ResponseEntity.status(HttpStatus.OK).body(booksDTO);
    }

    @GetMapping("/name/{nameBook}")
    public ResponseEntity<BookResponseDTO> getBookOfUserAndName(@Valid @RequestParam Integer idUser, @PathVariable String nameBook) {
        logger.debug("Appel de getBookOfUserAndName avec l id user "+idUser+" et le livre "+nameBook);
        Optional<Book> opBook = this.serviceBook.findBookByNomAndIdUtilisateur(nameBook, idUser);
        if (opBook.isEmpty()) {
            logger.debug("retour 404 de getBookOfUserAndName, le livre "+nameBook+" pour l utilisateur "+idUser+" est introuvable en bdd");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        logger.debug("retour 200 de getBookOfUserAndName avec pour l utilisateur "+idUser+" avec le livre : "+opBook.get());
        return ResponseEntity.status(HttpStatus.OK).body(ConverterEntityToResponseDTO.convertBookToBookDTO(opBook.get()));
    }

    @PutMapping("/updateBook")
    public ResponseEntity<BookResponseDTO> updateBook(@Valid @RequestBody BookRequestDTO bookRequestDTO) {
        try {
            logger.debug("Appel de updateBook avec le livre "+bookRequestDTO.toString());
            Book bookWithDataUpdate = ConvertRequestDTOToEntity.convertBookDTOToBook(bookRequestDTO);
            BookResponseDTO bookUpdateResponse = ConverterEntityToResponseDTO.convertBookToBookDTO(this.serviceBook.updateBook(bookWithDataUpdate, bookRequestDTO.getIdUser()));
            logger.debug("retour 200 de updateBook avec le livre "+bookUpdateResponse);
            return ResponseEntity.ok(bookUpdateResponse);
        } catch (BookNonTrouveException e) {
            logger.debug("retour 404 de updateBook, le livre "+bookRequestDTO.getId()+" est introuvable en bdd");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch(UtilisateurNonTrouveException e) {
            logger.debug("retour 404 de updateBook, l utilisateur " + bookRequestDTO.getIdUser() + " est introuvable en bdd");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch(BookExisteDejaException e) {
            logger.debug("retour 409 de updateBook, le livre "+bookRequestDTO.getNom()+" existe deja en bdd");
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (ParseException e) {
            logger.debug("retour 400 de updateBook, la date est non correct");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/deleteBook")
    public ResponseEntity<Void> deleteBook(@Valid @RequestParam Integer idLivre) {
        logger.debug("Appel de deleteBook avec l'id livre : "+idLivre);
        try {
            this.serviceBook.deleteBook(idLivre);
            logger.debug("retour 200 de deleteBook avec l'id livre "+idLivre);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (BookNonTrouveException e) {
            logger.debug("retour 404 de deleteBook avec l'id livre "+idLivre+" car non trouvé en bdd.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (BookNonSupprimeException e) {
            logger.debug("retour 409 de deleteBook avec l'id livre "+idLivre+" car non supprimé en bdd.");
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
