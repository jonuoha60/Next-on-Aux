package com.example.demo.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.helper.SecurityUtils;
import com.example.demo.model.RefreshToken;
import com.example.demo.model.Room;
import com.example.demo.model.User;
import com.example.demo.service.AuthService;
import com.example.demo.service.JWTService;
import com.example.demo.service.RefreshTokenService;
import com.example.demo.service.RoomService;
import com.example.demo.service.UserService;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;


@Controller
public class RoomController {
    private final UserService userService;
    private final RoomService roomService;
    private final JWTService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthService authService;

    @Value("${firebase.apiKey}")
    private String apiKey;

    @Value("${firebase.authDomain}")
    private String authDomain;

    @Value("${firebase.projectId}")
    private String projectId;

    @Value("${firebase.appId}")
    private String appId;
    

    @Autowired
    public RoomController(
        UserService userService,
        RoomService roomService,
        JWTService jwtService,
        RefreshTokenService refreshTokenService,
        AuthService authService
    ) {
        this.refreshTokenService = refreshTokenService;
        this.userService = userService;
        this.roomService = roomService;
        this.jwtService = jwtService;
        this.authService = authService;
    }

    @GetMapping("/")
    public String home(Model model) {
        // model.addAttribute("message", "Welcome to Thymeleaf!");
        return "home"; 
    }
    @GetMapping("/explore")
    public String explore(Model model) {
        return "explore"; 
    }

  @GetMapping("/create-battle")
    public String create(Model model) {
        Room room = new Room();

        // Default to public room
        room.setPrivateRoom(false);

        model.addAttribute("room", room);

        System.out.println("Your private room " + room.getPrivateRoom());

        return "create";
    }

    @PostMapping("/save/create")
    public String saveCreate(
            @ModelAttribute("room") Room room,
            HttpServletRequest request,
            HttpSession session) {

        User user = authService.getCurrentUser(request, session);

        if (user == null) {
            return "redirect:/login";
        }

        String roomCode = SecurityUtils.secureCode();

        room.setRoomCode(roomCode);
        room.setHost(user);

        room.getUsers().add(user);

        roomService.save(room);

        return "redirect:/spotify/login?roomCode="
                + roomCode
                + "&roomName="
                + room.getRoomName();
    }

    @PostMapping("/save/join")
    public String saveJoin(
            @ModelAttribute("room") Room room,
            HttpServletRequest request,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User user = authService.getCurrentUser(request, session);

        if (user == null) {
            return "redirect:/login";
        }

        boolean joined = roomService.updateRoom(
                room.getRoomCode(),
                user
        );

        if (!joined) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Room is full"
            );

            return "redirect:/join";
        }

        return "redirect:/room/"
                + room.getRoomCode()
                + "/"
                + room.getRoomName();
    }

    

    @GetMapping("/join-battle")
    public String join(Model model) {
        model.addAttribute("room", new Room());

        return "join"; 
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("user", new User());

        return "login"; 
    }



    @GetMapping("/signup")
    public String signup(Model model) {

        model.addAttribute("firebaseApiKey", apiKey);
        model.addAttribute("firebaseAuthDomain", authDomain);
        model.addAttribute("firebaseProjectId", projectId);
        model.addAttribute("firebaseAppId", appId);

        model.addAttribute("user", new User());

        return "signup";
    }

    @PostMapping("/save/user/signup")
    public String saveUserSignup(@ModelAttribute User user, Model model) {
       try {
        userService.saveUser(user);
        return "redirect:/login";
       } catch(IllegalArgumentException e) {
        model.addAttribute("error", e.getMessage());
        model.addAttribute("user", user);
        return "signup";
       }
    }

@PostMapping("/save/user/login")
public String saveUserLogin(@RequestParam String email,
                            @RequestParam String password,
                            Model model,
                            HttpSession session,
                            HttpServletResponse response) {

    try {
        User user = userService.loginUser(email, password);

        Dotenv dotenv = Dotenv.load();

        String accessSecret = dotenv.get("ACCESS_TOKEN_SECRET");
        String refreshSecret = dotenv.get("REFRESH_TOKEN_SECRET");

        // Generate tokens
        String refreshToken = jwtService.generateRefreshToken(email, refreshSecret);
        String accessToken = jwtService.generateAccessToken(email, accessSecret);

        Instant expiryDate = Instant.now().plus(7, ChronoUnit.DAYS);

        // Save refresh token in DB
        RefreshToken token = new RefreshToken();
        token.setToken(refreshToken);
        token.setExpiryDate(expiryDate);
        token.setUser(user);

        refreshTokenService.saveToken(token);

        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false); 
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60); 

        response.addCookie(refreshCookie);

        session.setAttribute("user", user);
        session.setAttribute("accessToken", accessToken);

        return "redirect:/";

    } catch (IllegalArgumentException e) {
        model.addAttribute("error", e.getMessage());
        model.addAttribute("user", new User());
        return "login";
    }
}
    
    @GetMapping("/room/{id}/{name}")
    public String room(@PathVariable String id, @PathVariable String name, Model model) {
        Room room = roomService.findByRoomCode(id);
        model.addAttribute("room", room);

        return "room"; 
    }

    @GetMapping("/addToPlaylist")
    public String addSong() {
        return "";
    }





}
