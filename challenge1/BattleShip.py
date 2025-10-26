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

class Ship:
    def __init__(self, positions, length):
        self.positions = positions  # List of (x,y) tuples
        self.length = length
        self.hits = set()  # Positions that have been hit
        
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
        self.screen = pygame.display.set_mode((WINDOW_SIZE * 2 + 50, WINDOW_SIZE + 150))
        pygame.display.set_caption("AI Battleship - Powered by Gemini")
        self.font = pygame.font.Font(None, 36)
        self.small_font = pygame.font.Font(None, 24)
        self.big_font = pygame.font.Font(None, 48)
        
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
        
        # Initialize ships
        self.place_ships()
        
        # Gemini Setup
        genai.configure(api_key=os.getenv("..."))
        self.model = genai.GenerativeModel('gemini-pro')
        
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
        {{"x": 0, "y": 0, "strategy": "Why this position"}}
        
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
                print(f"🤖 AI fires at ({move['x']}, {move['y']}): {move.get('strategy', '')}")
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
                return {"x": x, "y": y, "strategy": "Targeting adjacent to hit"}
        
        # Otherwise use hunt pattern
        x, y = random.choice(available)
        return {"x": x, "y": y, "strategy": "Searching for ships"}
    
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
            self.player_shots.append((x, y))
        else:
            ships = self.player_ships
            self.ai_shots.append((x, y))
        
        # Check for hit
        for ship in ships:
            if ship.is_hit(x, y):
                ship.add_hit(x, y)
                if is_player:
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
        ai_thinking = False
        
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
                            result, ship_size = self.process_shot(grid_x, grid_y, True)
                            
                            if result == "hit":
                                self.last_hit_message = f"💥 HIT at ({grid_x}, {grid_y})!"
                                self.status_message = "HIT! Fire again!"
                                print(f"Player HIT at ({grid_x}, {grid_y})!")
                            elif result == "sunk":
                                self.last_hit_message = f"🎯 SUNK {ship_size}-unit ship!"
                                self.status_message = f"SHIP SUNK! (Size {ship_size})"
                                print(f"Player SUNK a {ship_size}-unit ship!")
                            else:
                                self.last_hit_message = f"💨 Miss at ({grid_x}, {grid_y})"
                                self.status_message = "Miss - AI's turn"
                                self.turn = "ai"
                                ai_thinking = False
                            
                            # Check for win
                            winner = self.check_game_over()
                            if winner:
                                self.game_over = True
                                self.status_message = f"🏆 {winner} WINS!"
            
            # AI Turn
            if self.turn == "ai" and not ai_thinking and not self.game_over:
                ai_thinking = True
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
                    
                    time.sleep(0.5)  # Brief pause for drama
                    self.turn = "player"
                    self.status_message = "Your turn - Click enemy grid to fire!"
                    ai_thinking = False
                    
                    # Check for win
                    winner = self.check_game_over()
                    if winner:
                        self.game_over = True
                        self.status_message = f"🏆 {winner} WINS!"
            
            # DRAW EVERYTHING
            self.screen.fill(WHITE)
            
            # Draw player grid (left)
            self.draw_grid(0, 0, "YOUR FLEET", True)
            
            # Draw enemy grid (right)
            self.draw_grid(WINDOW_SIZE + 50, 0, "ENEMY WATERS", False)
            
            # Draw status bar
            pygame.draw.rect(self.screen, GRAY, 
                           (0, WINDOW_SIZE, WINDOW_SIZE * 2 + 50, 150))
            
            # Status text
            status_surface = self.font.render(self.status_message, True, BLACK)
            self.screen.blit(status_surface, (10, WINDOW_SIZE + 10))
            
            # Last hit message
            hit_surface = self.small_font.render(self.last_hit_message, True, RED if "HIT" in self.last_hit_message or "SUNK" in self.last_hit_message else BLACK)
            self.screen.blit(hit_surface, (10, WINDOW_SIZE + 50))
            
            # Turn indicator
            if not self.game_over:
                turn_text = f"Turn: {'YOUR' if self.turn == 'player' else 'AI'}"
                turn_surface = self.font.render(turn_text, True, 
                                               BLUE if self.turn == 'player' else RED)
                self.screen.blit(turn_surface, (10, WINDOW_SIZE + 90))
            
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
                for pos in ship.positions:
                    x, y = pos
                    rect = pygame.Rect(offset_x + x * CELL_SIZE + 5, 
                                     offset_y + y * CELL_SIZE + 5,
                                     CELL_SIZE - 10, CELL_SIZE - 10)
                    if pos in ship.hits:
                        pygame.draw.rect(self.screen, ORANGE, rect)
                    else:
                        pygame.draw.rect(self.screen, DARK_GRAY, rect)
            
            # Draw enemy shots on player grid
            for shot in self.ai_shots:
                x, y = shot
                center = (offset_x + x * CELL_SIZE + CELL_SIZE // 2,
                         offset_y + y * CELL_SIZE + CELL_SIZE // 2)
                if shot in self.ai_hits:
                    # Hit marker
                    pygame.draw.line(self.screen, RED,
                                   (center[0] - 20, center[1] - 20),
                                   (center[0] + 20, center[1] + 20), 4)
                    pygame.draw.line(self.screen, RED,
                                   (center[0] + 20, center[1] - 20),
                                   (center[0] - 20, center[1] + 20), 4)
                else:
                    # Miss marker
                    pygame.draw.circle(self.screen, BLUE, center, 10)
        else:
            # Enemy grid - show player's shots
            for shot in self.player_shots:
                x, y = shot
                center = (offset_x + x * CELL_SIZE + CELL_SIZE // 2,
                         offset_y + y * CELL_SIZE + CELL_SIZE // 2)
                if shot in self.player_hits:
                    # Hit marker
                    pygame.draw.circle(self.screen, RED, center, 20)
                    pygame.draw.line(self.screen, BLACK,
                                   (center[0] - 15, center[1] - 15),
                                   (center[0] + 15, center[1] + 15), 3)
                    pygame.draw.line(self.screen, BLACK,
                                   (center[0] + 15, center[1] - 15),
                                   (center[0] - 15, center[1] + 15), 3)
                else:
                    # Miss marker
                    pygame.draw.circle(self.screen, BLUE, center, 10)

if __name__ == "__main__":
    print("=" * 50)
    print("🚢 BATTLESHIP - AI vs Human")
    print("=" * 50)
    print("Ships: One 3-unit ship, One 2-unit ship")
    print("Click the enemy grid to fire!")
    print("-" * 50)
    
    game = GridGame()
    game.run()
