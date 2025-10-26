import pygame
import json
import google.generativeai as genai
from dotenv import load_dotenv
import os
import time
import random

load_dotenv()

# CONSTANTS
GRID_SIZE = 5
CELL_SIZE = 100
WINDOW_SIZE = GRID_SIZE * CELL_SIZE
FPS = 60

# Colors
WHITE = (255, 255, 255)
BLACK = (0, 0, 0)
BLUE = (0, 100, 255)
RED = (255, 50, 50)
GREEN = (0, 200, 0)
GRAY = (200, 200, 200)
DARK_GRAY = (100, 100, 100)
YELLOW = (255, 255, 0)
ORANGE = (255, 165, 0)
LIGHT_BLUE = (173, 216, 230)
NAVY_BLUE = (0, 0, 128)
BROWN = (101, 67, 33)

class Ship:
    def __init__(self, positions, length):
        self.positions = positions  # List of (x,y) tuples
        self.length = length
        self.hits = set()  # Positions that have been hit
        # Determine orientation
        if len(positions) > 1:
            self.horizontal = positions[0][1] == positions[1][1]
        else:
            self.horizontal = True
        
    def is_hit(self, x, y):
        return (x, y) in self.positions
    
    def add_hit(self, x, y):
        if (x, y) in self.positions:
            self.hits.add((x, y))
            return True
        return False
    
    def is_sunk(self):
        return len(self.hits) == self.length

class GridGame:
    def __init__(self):
        pygame.init()
        self.screen = pygame.display.set_mode((WINDOW_SIZE * 2 + 50, WINDOW_SIZE + 250))
        pygame.display.set_caption("AI Battleship - Powered by Gemini")
        self.font = pygame.font.Font(None, 36)
        self.small_font = pygame.font.Font(None, 24)
        self.big_font = pygame.font.Font(None, 48)
        self.tiny_font = pygame.font.Font(None, 20)
        
        # Game state
        self.player_ships = []
        self.ai_ships = []
        self.player_shots = []  # List of (x,y) positions player has shot at
        self.ai_shots = []      # List of (x,y) positions AI has shot at
        self.player_hits = []   # Successful hits by player
        self.ai_hits = []       # Successful hits by AI
        self.turn = "player"
        self.move_history = []
        self.status_message = "Your turn - Click enemy grid to fire!"
        self.last_hit_message = ""
        self.game_over = False
        self.ai_thinking = False
        self.ai_strategy = "Analyzing battlefield..."
        self.thinking_dots = 0
        self.thinking_timer = 0
        self.pending_player_shot = None  # Store player's shot to process after showing
        
        # Initialize ships
        self.place_ships()
        
        # Gemini Setup
        genai.configure(api_key=os.getenv("GEMINI_API_KEY"))
        self.model = genai.GenerativeModel('gemini-2.5-pro')
        
        print("🚢 BATTLESHIP STARTED! AI is powered by Google Gemini")
        print(f"Player ships placed at: {[ship.positions for ship in self.player_ships]}")
        print(f"AI ships placed at: {[ship.positions for ship in self.ai_ships]}")
        
    def place_ships(self):
        """Randomly place ships for both players"""
        # Place player ships
        self.player_ships = self.generate_random_ships()
        # Place AI ships
        self.ai_ships = self.generate_random_ships()
        
    def generate_random_ships(self):
        """Generate random ship positions"""
        ships = []
        occupied = set()
        
        # Ship sizes: one of length 3, one of length 2
        ship_sizes = [3, 2]
        
        for size in ship_sizes:
            placed = False
            attempts = 0
            while not placed and attempts < 100:
                attempts += 1
                orientation = random.choice(['horizontal', 'vertical'])
                
                if orientation == 'horizontal':
                    x = random.randint(0, GRID_SIZE - size)
                    y = random.randint(0, GRID_SIZE - 1)
                    positions = [(x + i, y) for i in range(size)]
                else:  # vertical
                    x = random.randint(0, GRID_SIZE - 1)
                    y = random.randint(0, GRID_SIZE - size)
                    positions = [(x, y + i) for i in range(size)]
                
                # Check if positions are free
                if not any(pos in occupied for pos in positions):
                    ships.append(Ship(positions, size))
                    occupied.update(positions)
                    placed = True
        
        return ships
    
    def draw_ship_part(self, surface, x, y, width, height, is_front, is_back, horizontal, is_hit):
        """Draw a single part of a ship"""
        # Choose colors based on hit status
        hull_color = ORANGE if is_hit else DARK_GRAY
        deck_color = RED if is_hit else GRAY
        
        if horizontal:
            # Ship body
            body_rect = pygame.Rect(x + 10, y + 30, width - 20, height - 60)
            pygame.draw.rect(surface, hull_color, body_rect)
            
            # Deck
            deck_rect = pygame.Rect(x + 15, y + 35, width - 30, height - 70)
            pygame.draw.rect(surface, deck_color, deck_rect)
            
            # Front (bow)
            if is_front:
                bow_points = [
                    (x + width - 10, y + 30),
                    (x + width + 10, y + height // 2),
                    (x + width - 10, y + height - 30)
                ]
                pygame.draw.polygon(surface, hull_color, bow_points)
            
            # Back (stern)
            if is_back:
                stern_rect = pygame.Rect(x - 5, y + 35, 20, height - 70)
                pygame.draw.rect(surface, hull_color, stern_rect)
                # Propeller area
                pygame.draw.circle(surface, BROWN, (x + 5, y + height // 2), 8)
            
            # Details
            if not is_hit:
                # Windows
                for i in range(2):
                    pygame.draw.circle(surface, LIGHT_BLUE, 
                                     (x + 30 + i * 30, y + height // 2), 5)
        else:  # Vertical
            # Ship body
            body_rect = pygame.Rect(x + 30, y + 10, width - 60, height - 20)
            pygame.draw.rect(surface, hull_color, body_rect)
            
            # Deck
            deck_rect = pygame.Rect(x + 35, y + 15, width - 70, height - 30)
            pygame.draw.rect(surface, deck_color, deck_rect)
            
            # Front (bow)
            if is_front:
                bow_points = [
                    (x + 30, y + height - 10),
                    (x + width // 2, y + height + 10),
                    (x + width - 30, y + height - 10)
                ]
                pygame.draw.polygon(surface, hull_color, bow_points)
            
            # Back (stern)
            if is_back:
                stern_rect = pygame.Rect(x + 35, y - 5, width - 70, 20)
                pygame.draw.rect(surface, hull_color, stern_rect)
                # Propeller area
                pygame.draw.circle(surface, BROWN, (x + width // 2, y + 5), 8)
            
            # Details
            if not is_hit:
                # Windows
                for i in range(2):
                    pygame.draw.circle(surface, LIGHT_BLUE, 
                                     (x + width // 2, y + 30 + i * 30), 5)
    
    def get_llm_move(self):
        """Get move from Gemini AI"""
        # Build game state for LLM
        state_prompt = f"""
        You are playing Battleship. You need to find and sink the enemy ships.
        
        Game State:
        - Grid Size: {GRID_SIZE}x{GRID_SIZE} (0-indexed)
        - Your previous shots: {self.ai_shots}
        - Your hits: {self.ai_hits}
        - Shots you haven't tried yet: {self.get_ai_available_shots()}
        
        Strategy tips:
        1. If you got a hit, try adjacent squares
        2. Use a search pattern to find ships
        3. Ships are either horizontal or vertical
        4. There are 2 ships: one of length 3, one of length 2
        
        Respond with ONLY a valid JSON object:
        {{"x": 0, "y": 0, "strategy": "Your reasoning in 10 words or less"}}
        
        The x,y must be between 0-{GRID_SIZE-1} and not already shot at.
        """
        
        try:
            response = self.model.generate_content(state_prompt)
            response_text = response.text.strip()
            
            # Clean up response
            if "```json" in response_text:
                response_text = response_text.split("```json")[1].split("```")[0]
            elif "```" in response_text:
                response_text = response_text.split("```")[1].split("```")[0]
            
            move = json.loads(response_text)
            
            # Validate move
            if (0 <= move["x"] < GRID_SIZE and 
                0 <= move["y"] < GRID_SIZE and 
                (move["x"], move["y"]) not in self.ai_shots):
                self.ai_strategy = move.get("strategy", "Calculating optimal target...")
                print(f"🤖 AI fires at ({move['x']}, {move['y']}): {self.ai_strategy}")
                return move
            else:
                return self.get_fallback_ai_shot()
                
        except Exception as e:
            print(f"LLM Error: {e}")
            return self.get_fallback_ai_shot()
    
    def get_fallback_ai_shot(self):
        """Smart fallback when LLM fails"""
        available = self.get_ai_available_shots()
        if not available:
            return None
        
        # If we have recent hits, try adjacent squares
        if self.ai_hits:
            last_hit = self.ai_hits[-1]
            adjacents = [
                (last_hit[0]+1, last_hit[1]),
                (last_hit[0]-1, last_hit[1]),
                (last_hit[0], last_hit[1]+1),
                (last_hit[0], last_hit[1]-1)
            ]
            valid_adjacents = [pos for pos in adjacents if pos in available]
            if valid_adjacents:
                x, y = random.choice(valid_adjacents)
                self.ai_strategy = "Targeting adjacent to previous hit"
                return {"x": x, "y": y, "strategy": self.ai_strategy}
        
        # Otherwise use hunt pattern
        x, y = random.choice(available)
        self.ai_strategy = "Searching for enemy ships"
        return {"x": x, "y": y, "strategy": self.ai_strategy}
    
    def get_ai_available_shots(self):
        """Get list of positions AI hasn't shot at yet"""
        available = []
        for x in range(GRID_SIZE):
            for y in range(GRID_SIZE):
                if (x, y) not in self.ai_shots:
                    available.append((x, y))
        return available
    
    def process_shot(self, x, y, is_player):
        """Process a shot and return result"""
        if is_player:
            ships = self.ai_ships
            # Don't append here - already done when showing the shot
            if (x, y) not in self.player_shots:
                self.player_shots.append((x, y))
        else:
            ships = self.player_ships
            self.ai_shots.append((x, y))
        
        # Check for hit
        for ship in ships:
            if ship.is_hit(x, y):
                ship.add_hit(x, y)
                if is_player:
                    if (x, y) not in self.player_hits:
                        self.player_hits.append((x, y))
                else:
                    self.ai_hits.append((x, y))
                
                if ship.is_sunk():
                    return "sunk", ship.length
                return "hit", None
        
        return "miss", None
    
    def check_game_over(self):
        """Check if all ships of either player are sunk"""
        player_all_sunk = all(ship.is_sunk() for ship in self.player_ships)
        ai_all_sunk = all(ship.is_sunk() for ship in self.ai_ships)
        
        if player_all_sunk:
            return "AI"
        elif ai_all_sunk:
            return "Player"
        return None
    
    def run(self):
        clock = pygame.time.Clock()
        running = True
        
        while running:
            for event in pygame.event.get():
                if event.type == pygame.QUIT:
                    running = False
                
                if event.type == pygame.MOUSEBUTTONDOWN and self.turn == "player" and not self.game_over:
                    x, y = event.pos
                    # Check if click is on enemy grid (right side)
                    if WINDOW_SIZE + 50 <= x <= WINDOW_SIZE * 2 + 50 and y < WINDOW_SIZE:
                        grid_x = (x - WINDOW_SIZE - 50) // CELL_SIZE
                        grid_y = y // CELL_SIZE
                        
                        # Check if not already shot
                        if (grid_x, grid_y) not in self.player_shots:
                            # Immediately add the shot to show it
                            self.player_shots.append((grid_x, grid_y))
                            self.pending_player_shot = (grid_x, grid_y)
                            self.status_message = "Processing shot..."
            
            # Process player's shot after showing it
            if self.pending_player_shot and self.turn == "player":
                x, y = self.pending_player_shot
                result, ship_size = self.process_shot(x, y, True)
                
                if result == "hit":
                    self.last_hit_message = f"💥 HIT at ({x}, {y})!"
                    self.status_message = "HIT! Fire again!"
                    print(f"Player HIT at ({x}, {y})!")
                    # Keep turn as player for another shot
                elif result == "sunk":
                    self.last_hit_message = f"🎯 SUNK {ship_size}-unit ship!"
                    self.status_message = f"SHIP SUNK! (Size {ship_size})"
                    print(f"Player SUNK a {ship_size}-unit ship!")
                    # Keep turn as player
                else:
                    self.last_hit_message = f"💨 Miss at ({x}, {y})"
                    self.status_message = "Miss - AI is thinking..."
                    self.turn = "ai"
                    self.ai_thinking = True
                    self.ai_strategy = "Analyzing battlefield..."
                
                self.pending_player_shot = None
                
                # Check for win
                winner = self.check_game_over()
                if winner:
                    self.game_over = True
                    self.status_message = f"🏆 {winner} WINS!"
            
            # AI Turn with delay for thinking animation
            if self.turn == "ai" and self.ai_thinking and not self.game_over:
                self.thinking_timer += 1
                
                # Wait a bit for dramatic effect
                if self.thinking_timer > 60:  # About 1 second at 60 FPS
                    move = self.get_llm_move()
                    if move:
                        result, ship_size = self.process_shot(move["x"], move["y"], False)
                        
                        if result == "hit":
                            self.last_hit_message = f"AI HIT at ({move['x']}, {move['y']})!"
                            print(f"AI HIT at ({move['x']}, {move['y']})!")
                        elif result == "sunk":
                            self.last_hit_message = f"AI SUNK your {ship_size}-unit ship!"
                            print(f"AI SUNK your {ship_size}-unit ship!")
                        else:
                            self.last_hit_message = f"AI missed at ({move['x']}, {move['y']})"
                        
                        self.turn = "player"
                        self.status_message = "Your turn - Click enemy grid to fire!"
                        self.ai_thinking = False
                        self.thinking_timer = 0
                        
                        # Check for win
                        winner = self.check_game_over()
                        if winner:
                            self.game_over = True
                            self.status_message = f"🏆 {winner} WINS!"
            
            # Update thinking animation
            if self.ai_thinking:
                self.thinking_dots = (self.thinking_dots + 1) % 40
            
            # DRAW EVERYTHING
            self.screen.fill(WHITE)
            
            # Draw player grid (left)
            self.draw_grid(0, 0, "YOUR FLEET", True)
            
            # Draw enemy grid (right)
            self.draw_grid(WINDOW_SIZE + 50, 0, "ENEMY WATERS", False)
            
            # Draw AI Strategy Box
            strategy_box = pygame.Rect(WINDOW_SIZE + 60, WINDOW_SIZE + 10, WINDOW_SIZE - 20, 80)
            pygame.draw.rect(self.screen, LIGHT_BLUE, strategy_box)
            pygame.draw.rect(self.screen, BLACK, strategy_box, 2)
            
            # AI Strategy Text
            strategy_label = self.tiny_font.render("AI STRATEGY:", True, BLACK)
            self.screen.blit(strategy_label, (WINDOW_SIZE + 70, WINDOW_SIZE + 20))
            
            # Show thinking animation or strategy
            if self.ai_thinking:
                dots = "." * (self.thinking_dots // 10)
                thinking_text = f"Calculating{dots}"
                thinking_surface = self.small_font.render(thinking_text, True, BLACK)
                self.screen.blit(thinking_surface, (WINDOW_SIZE + 70, WINDOW_SIZE + 45))
            else:
                # Word wrap the strategy text
                words = self.ai_strategy.split(' ')
                lines = []
                current_line = ""
                for word in words:
                    test_line = current_line + " " + word if current_line else word
                    if len(test_line) < 30:
                        current_line = test_line
                    else:
                        if current_line:
                            lines.append(current_line)
                        current_line = word
                if current_line:
                    lines.append(current_line)
                
                for i, line in enumerate(lines[:2]):  # Max 2 lines
                    strategy_surface = self.small_font.render(line, True, BLACK)
                    self.screen.blit(strategy_surface, (WINDOW_SIZE + 70, WINDOW_SIZE + 45 + i*25))
            
            # Draw status bar
            pygame.draw.rect(self.screen, GRAY, 
                           (0, WINDOW_SIZE, WINDOW_SIZE + 40, 150))
            
            # Status text
            status_surface = self.font.render(self.status_message, True, BLACK)
            self.screen.blit(status_surface, (10, WINDOW_SIZE + 110))
            
            # Last hit message
            hit_surface = self.small_font.render(self.last_hit_message, True, 
                                                RED if "HIT" in self.last_hit_message or "SUNK" in self.last_hit_message else BLACK)
            self.screen.blit(hit_surface, (10, WINDOW_SIZE + 150))
            
            # Turn indicator
            if not self.game_over:
                turn_text = f"Turn: {'YOUR' if self.turn == 'player' else 'AI'}"
                turn_surface = self.font.render(turn_text, True, 
                                               BLUE if self.turn == 'player' else RED)
                self.screen.blit(turn_surface, (10, WINDOW_SIZE + 190))
            
            pygame.display.flip()
            clock.tick(FPS)
        
        pygame.quit()
    
    def draw_grid(self, offset_x, offset_y, title, show_ships):
        """Draw a game grid"""
        # Title
        title_surface = self.small_font.render(title, True, BLACK)
        self.screen.blit(title_surface, (offset_x + WINDOW_SIZE//2 - 60, offset_y - 25))
        
        # Grid lines
        for i in range(GRID_SIZE + 1):
            pygame.draw.line(self.screen, GRAY, 
                           (offset_x + i * CELL_SIZE, offset_y), 
                           (offset_x + i * CELL_SIZE, offset_y + WINDOW_SIZE), 2)
            pygame.draw.line(self.screen, GRAY, 
                           (offset_x, offset_y + i * CELL_SIZE), 
                           (offset_x + WINDOW_SIZE, offset_y + i * CELL_SIZE), 2)
        
        # Draw ships (only on player grid)
        if show_ships:
            for ship in self.player_ships:
                for i, pos in enumerate(ship.positions):
                    x, y = pos
                    is_front = (i == len(ship.positions) - 1)
                    is_back = (i == 0)
                    is_hit = pos in ship.hits
                    
                    self.draw_ship_part(
                        self.screen,
                        offset_x + x * CELL_SIZE,
                        offset_y + y * CELL_SIZE,
                        CELL_SIZE,
                        CELL_SIZE,
                        is_front,
                        is_back,
                        ship.horizontal,
                        is_hit
                    )
            
            # Draw enemy shots on player grid
            for shot in self.ai_shots:
                x, y = shot
                center = (offset_x + x * CELL_SIZE + CELL_SIZE // 2,
                         offset_y + y * CELL_SIZE + CELL_SIZE // 2)
                if shot in self.ai_hits:
                    # Hit marker - explosion
                    for radius in [15, 25, 35]:
                        alpha = 255 - (radius * 5)
                        color = (255, 200 - radius*2, 0)
                        pygame.draw.circle(self.screen, color, center, radius, 2)
                else:
                    # Miss marker - water splash
                    pygame.draw.circle(self.screen, BLUE, center, 12)
                    pygame.draw.circle(self.screen, LIGHT_BLUE, center, 8)
                    pygame.draw.circle(self.screen, WHITE, center, 4)
        else:
            # Enemy grid - show player's shots
            for shot in self.player_shots:
                x, y = shot
                center = (offset_x + x * CELL_SIZE + CELL_SIZE // 2,
                         offset_y + y * CELL_SIZE + CELL_SIZE // 2)
                if shot in self.player_hits:
                    # Hit marker - explosion
                    pygame.draw.circle(self.screen, RED, center, 25)
                    pygame.draw.circle(self.screen, ORANGE, center, 18)
                    pygame.draw.circle(self.screen, YELLOW, center, 10)
                else:
                    # Miss marker - water splash
                    pygame.draw.circle(self.screen, BLUE, center, 12)
                    pygame.draw.circle(self.screen, LIGHT_BLUE, center, 8)
                    pygame.draw.circle(self.screen, WHITE, center, 4)

if __name__ == "__main__":
    print("=" * 50)
    print("🚢 BATTLESHIP - AI vs Human")
    print("=" * 50)
    print("Ships: One 3-unit ship, One 2-unit ship")
    print("Click the enemy grid to fire!")
    print("-" * 50)
    
    game = GridGame()
    game.run()