//
//import de.ruoff.consistency.service.auth.AuthModel
//import de.ruoff.consistency.service.auth.AuthRepository
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
//
//class AuthService(val AuthRepository : AuthRepository) {
//    private val encoder = BCryptPasswordEncoder()
//
//    fun hashPassword(password: String): String {
//        return encoder.encode(password)
//    }
//
//    fun verifyPassword (plainPassword: String, hashedPassword: String): Boolean {
//        return encoder.matches(plainPassword, hashedPassword)
//    }
//    fun register(username: String, password: String): AuthModel {
//        if (AuthRepository.existsByUserName(username)){
//            throw IllegalArgumentException("Benutzername existiert bereits!")
//        }
//        val hashedPassword = hashPassword(password)
//
//        val user = AuthModel(username = username, password = hashedPassword)
//        return AuthRepository.save(user)
//    }
//
//    fun login(username: String, password: String): String{
//        val user = AuthRepository.findByUserName(username)
//            ?: throw  NoSuchElementException("Nutzer existiert nicht")
//
//
//        if(!verifyPassword(password, user.password)) {
//            throw IllegalArgumentException("Falsches Passwort")
//        }
//        return "Login erfolgreich!"
//
//    }
//
//
//
//
//}