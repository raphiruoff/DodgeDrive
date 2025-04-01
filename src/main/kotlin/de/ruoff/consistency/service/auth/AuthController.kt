package de.ruoff.consistency.service.auth

import io.grpc.stub.StreamObserver
import org.springframework.grpc.server.service.GrpcService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import io.grpc.Status
import io.grpc.StatusRuntimeException

@GrpcService
class AuthController(
    val authRepository: AuthRepository,
    private val jwtService: JwtService
):AuthServiceGrpc.AuthServiceImplBase() {
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
                val status = Status.ALREADY_EXISTS
                    .withDescription("Benutzername bereits vergeben.")
                    .asRuntimeException()
                responseObserver.onError(status)
                return
            }

            val hashedPassword = hashPassword(request.password)
            val user = AuthModel(username = request.username, password = hashedPassword)
            authRepository.save(user)

            val response = RegisterResponse.newBuilder()
                .setMessage("Registrierung erfolgreich")
                .build()

            responseObserver.onNext(response)
            responseObserver.onCompleted()

        } catch (e: Exception) {
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Fehler bei Registrierung: ${e.message}")
                    .asRuntimeException()
            )
        }
    }

    override fun login(request: LoginRequest, responseObserver: StreamObserver<LoginResponse>) {
        try {
            val user = authRepository.findByUsername(request.username)

            if (user == null) {
                responseObserver.onError(
                    Status.NOT_FOUND
                        .withDescription("Kein User unter diesem Namen vorhanden.")
                        .asRuntimeException()
                )
                return
            }

            if (!verifyPassword(request.password, user.password)) {
                responseObserver.onError(
                    Status.PERMISSION_DENIED
                        .withDescription("Falsches Passwort. Bitte erneut eingeben.")
                        .asRuntimeException()
                )
                return
            }

            val token = jwtService.generateToken(request.username)



            val response = LoginResponse.newBuilder()
                .setMessage("Login erfolgreich!")
                .setToken(token)
                .build()


            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Fehler beim Login: ${e.message}")
                    .asRuntimeException()
            )
        }
    }





}
