/**
 * Table displaying manageable users with delete action
 */

import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Tooltip,
  Typography,
} from "@mui/material";
import DeleteIcon from "@mui/icons-material/Delete";
import type { UserInfo } from "@/types";

interface UserTableProps {
  users: UserInfo[];
  onDelete: (username: string) => void;
}

export const UserTable = ({ users, onDelete }: UserTableProps) => {
  if (users.length === 0) {
    return (
      <Typography color="text.secondary" sx={{ mt: 2 }}>
        No users found.
      </Typography>
    );
  }

  return (
    <TableContainer component={Paper} variant="outlined">
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Username</TableCell>
            <TableCell>Email</TableCell>
            <TableCell>First Name</TableCell>
            <TableCell>Last Name</TableCell>
            <TableCell align="right">Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {users.map((user) => (
            <TableRow key={user.id} hover>
              <TableCell>{user.username}</TableCell>
              <TableCell>{user.email}</TableCell>
              <TableCell>{user.firstName}</TableCell>
              <TableCell>{user.lastName}</TableCell>
              <TableCell align="right">
                <Tooltip title="Delete user">
                  <IconButton
                    color="error"
                    size="small"
                    onClick={() => onDelete(user.username)}
                  >
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
};
