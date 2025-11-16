import java.io.*;
import java.util.*;

/**
 * MemoryAllocationLab.java
 * Simple memory allocator simulator (First-Fit).
 *
 * Input file format:
 * - First non-empty line: total memory (integer)
 * - Following lines:
 *    <name> <size>       e.g. P1 100   (allocate)
 *    FREE <name>         e.g. FREE P1  (deallocate)
 *    D <name> or DEALLOC <name> also accepted
 *
 * Prints actions, block table after each action, and final stats.
 */

public class MemoryAllocationLab {

    static class Block {
        int start;      // starting address 
        int size;       // block size
        String proc;    // null if free, otherwise the process name

        Block(int start, int size, String proc) {
            this.start = start;
            this.size = size;
            this.proc = proc;
        }

        boolean isFree() {
            return proc == null;
        }

        @Override
        public String toString() {
            if (isFree()) return String.format("[Free  start=%d size=%d]", start, size);
            else return String.format("[%s start=%d size=%d]", proc, start, size);
        }
    }

    private int totalMemory;
    private LinkedList<Block> blocks; // maintain order in memory
    private int allocSuccess = 0;
    private int allocFail = 0;

    public MemoryAllocationLab(int totalMemory) {
        this.totalMemory = totalMemory;
        blocks = new LinkedList<>();
        blocks.add(new Block(0, totalMemory, null)); 
    }

    // First-Fit allocation
    public boolean allocateFirstFit(String procName, int reqSize) {
        ListIterator<Block> it = blocks.listIterator();
        while (it.hasNext()) {
            Block b = it.next();
            if (b.isFree() && b.size >= reqSize) {
                int originalStart = b.start;
                // If exactly fits just occupy block
                if (b.size == reqSize) {
                    b.proc = procName;
                } else {
                    
                    int leftoverSize = b.size - reqSize;
                    Block allocated = new Block(b.start, reqSize, procName);
                    Block leftover = new Block(b.start + reqSize, leftoverSize, null);
                    
                    it.remove();            // remove b
                    it.add(allocated);      
                    it.add(leftover);       
                }
                System.out.printf("ALLOCATE %s %d -> success (used start=%d)\n", procName, reqSize, originalStart);
                allocSuccess++;
                printBlocks();
                return true;
            }
        }
        System.out.printf("ALLOCATE %s %d -> FAIL (no single contiguous block big enough)\n", procName, reqSize);
        allocFail++;
        printBlocks();
        return false;
    }

    // Deallocation and merge adjacent free blocks
    public boolean deallocate(String procName) {
        ListIterator<Block> it = blocks.listIterator();
        boolean found = false;
        while (it.hasNext()) {
            Block b = it.next();
            if (!b.isFree() && b.proc.equals(procName)) {
                b.proc = null; // free it
                found = true;
                System.out.printf("DEALLOCATE %s -> success (freed start=%d size=%d)\n", procName, b.start, b.size);
                // attempt merge with previous
                if (it.hasPrevious()) {
                    
                }
                mergeAdjacent();
                printBlocks();
                break;
            }
        }
        if (!found) {
            System.out.printf("DEALLOCATE %s -> fail (process not found)\n", procName);
            printBlocks();
        }
        return found;
    }

    // Merge 
    private void mergeAdjacent() {
        ListIterator<Block> it = blocks.listIterator();
        while (it.hasNext()) {
            Block cur = it.next();
            if (!cur.isFree()) continue;
            // if next exists and is free merge
            if (it.hasNext()) {
                Block nxt = it.next();
                if (nxt.isFree()) {
                    
                    it.previous(); 
                    it.previous(); 
                    it.remove();   
                    it.add(new Block(cur.start, cur.size + nxt.size, null)); // merged block
                    it.next();     
                    it.next();     
                    it.remove();   
                    
                    it = blocks.listIterator();
                } else {
                    it.previous(); 
                }
            }
        }
      
        LinkedList<Block> merged = new LinkedList<>();
        for (Block b : blocks) {
            if (!merged.isEmpty() && merged.getLast().isFree() && b.isFree()) {
                Block last = merged.removeLast();
                merged.add(new Block(last.start, last.size + b.size, null));
            } else {
                merged.add(new Block(b.start, b.size, b.proc));
            }
        }
        
        int curr = 0;
        blocks.clear();
        for (Block b : merged) {
            blocks.add(new Block(curr, b.size, b.proc));
            curr += b.size;
        }
    }

   
    public void printBlocks() {
        System.out.println("Current memory map:");
        for (Block b : blocks) {
            System.out.println("  " + b.toString());
        }
        System.out.println();
    }

    // Compute stats
    public int totalFree() {
        int sum = 0;
        for (Block b : blocks) if (b.isFree()) sum += b.size;
        return sum;
    }

    public int largestFreeBlock() {
        int max = 0;
        for (Block b : blocks) if (b.isFree() && b.size > max) max = b.size;
        return max;
    }

    public void printFinalStats() {
        System.out.println("===== FINAL STATS =====");
        System.out.println("Total memory: " + totalMemory);
        int free = totalFree();
        int largest = largestFreeBlock();
        System.out.println("Total free: " + free);
        System.out.println("Largest free block: " + largest);
        double extFrag = 0.0;
        if (free > 0) extFrag = ((double)(free - largest) / totalMemory) * 100.0;
        System.out.printf("External fragmentation: %.2f%%\n", extFrag);
        System.out.printf("Alloc success: %d, failures: %d\n", allocSuccess, allocFail);
        System.out.println("========================");
    }

  
    public void runFromReader(BufferedReader br) throws IOException {
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] tok = line.split("\\s+");
            if (tok.length == 0) continue;

            if (tok.length == 1) {
                
                continue;
            } else if (tok.length >= 2) {
                
                String t0 = tok[0].toUpperCase();
                if (t0.equals("FREE") || t0.equals("D") || t0.equals("DEALLOC") || t0.equals("RELEASE")) {
                    String pname = tok[1];
                    deallocate(pname);
                    continue;
                }
                
                try {
                    int size = Integer.parseInt(tok[1]);
                    String pname = tok[0];
                    allocateFirstFit(pname, size);
                } catch (NumberFormatException nfe) {
                    
                    String t1 = tok[1].toUpperCase();
                    if (t1.equals("FREE") || t1.equals("D") || t1.equals("DEALLOC") || t1.equals("RELEASE")) {
                        deallocate(tok[0]);
                    } else {
                        System.out.println("Unrecognized line: " + line);
                    }
                }
            }
        }
    }

    // Run using file path
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java MemoryAllocationLab <input-file>");
            System.out.println("See top of file for accepted input formats.");
            return;
        }
        String path = args[0];
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            
            String first = null;
            while ((first = br.readLine()) != null) {
                first = first.trim();
                if (first.isEmpty() || first.startsWith("#")) continue;
                break;
            }
            if (first == null) {
                System.err.println("Empty input file or no valid total memory line.");
                return;
            }
            int total;
            try {
                total = Integer.parseInt(first.split("\\s+")[0]);
            } catch (NumberFormatException e) {
                System.err.println("First meaningful line must be an integer total memory. Found: " + first);
                return;
            }
            MemoryAllocationLab sim = new MemoryAllocationLab(total);
            System.out.printf("Initialized memory simulator with total=%d\n\n", total);
            
            try (BufferedReader br2 = new BufferedReader(new FileReader(path))) {
             
                String line;
                boolean skipped = false;
                while ((line = br2.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    if (!skipped) { skipped = true; continue; } 
                 
                    sim.runFromReader(new BufferedReader(new StringReader(line + "\n")));
                }
            }

    
            try (BufferedReader br3 = new BufferedReader(new FileReader(path))) {
               
                String l;
                boolean skipped = false;
                while ((l = br3.readLine()) != null) {
                    l = l.trim();
                    if (l.isEmpty() || l.startsWith("#")) continue;
                    if (!skipped) { skipped = true; continue; }
                    
                    sim.runFromReader(new BufferedReader(new StringReader(l + "\n")));
                }
            }

            System.out.println("\nAll requests processed.");
            sim.printFinalStats();

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }
}

