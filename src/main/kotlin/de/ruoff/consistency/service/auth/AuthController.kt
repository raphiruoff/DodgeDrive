package de.ruoff.consistency.service.auth

import io.grpc.stub.StreamObserver
import org.springframework.grpc.server.service.GrpcService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

@GrpcService
class AuthController(val authRepository: AuthRepository):AuthServiceGrpc.AuthServiceImplBase() {
    private val encoder = BCryptPasswordEncoder()

    fun hashPassword(password: String): String {
        return encoder.encode(password)
    }

    fun verifyPassword (plainPassword: String, hashedPassword: String): Boolean {
        return encoder.matches(plainPassword, hashedPassword)
    }
    override fun register(request: RegisterRequest, responseObserver: StreamObserver<RegisterResponse>) {
        try {
            if (authRepository.existsByUsername(request.username)) {
                responseObserver.onError(IllegalArgumentException("User existiert bereits!"))
                return
            }

            val hashedPassword = hashPassword(request.password)

            val user = AuthModel(username = request.username, password = hashedPassword)
            authRepository.save(user)

            // Antwort erstellen
            val response = RegisterResponse.newBuilder()
                .setMessage("Registrierung erfolgreich")
                .build()

            // Antwort an den gRPC-Client senden
            responseObserver.onNext(response)
            responseObserver.onCompleted()

        }
        catch (e: Exception) {
            responseObserver.onError(e)
        }

    }

    override fun login(request: LoginRequest, responseObserver: StreamObserver<LoginResponse>) {
        try {
            val user = authRepository.findByUsername(request.username)

            if (user == null) {
                responseObserver.onError(NoSuchElementException("Kein User unter diesem Namen vorhanden."))
                return
            }
            if (!verifyPassword(request.password, user.password)) {
                responseObserver.onError(IllegalArgumentException("Falsches Passwort. Bitte erneut eingeben."))
                return
            }

            val response = LoginResponse.newBuilder()
                .setMessage("Login erfolgreich!")
                .build()

            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(e)
        }
    }




}
