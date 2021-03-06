/******************************************************************************
 *  Copyright 2015 by OLTPBenchmark Project                                   *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *    http://www.apache.org/licenses/LICENSE-2.0                              *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 ******************************************************************************/

package com.oltpbenchmark.benchmarks.epinions.procedures;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.oltpbenchmark.api.Procedure;
import com.oltpbenchmark.api.SQLStmt;

public class GetReviewsByUser extends Procedure {
    
    // public final SQLStmt getReviewUser = new SQLStmt(
    //     "SELECT * FROM review r, useracct u WHERE u.u_id = r.u_id AND r.u_id=? "
    // );

    public final SQLStmt getReviewUser1 = new SQLStmt(
        "SELECT * FROM review WHERE u_id=?"
    );

    public final SQLStmt getReviewUser2 = new SQLStmt(
        "SELECT * FROM useracct WHERE u_id = ?"
    );

    public void run(Connection conn, long uid) throws SQLException {
        PreparedStatement stmt = this.getPreparedStatement(conn, getReviewUser2);
        stmt.setLong(1, uid);
        ResultSet r= stmt.executeQuery();
        while (r.next()) {
            continue;
        }
        r.close();
    }
}
